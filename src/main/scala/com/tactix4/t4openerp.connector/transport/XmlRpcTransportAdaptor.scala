/*
 * Copyright (C) 2013 Tactix4
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tactix4.t4openerp.connector.transport

import com.tactix4.t4xmlrpc._
import com.tactix4.t4xmlrpc.XmlRpcInt
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Failure, Success, Try}
import com.tactix4.t4openerp.connector._
import ExecutionContext.Implicits.global
import com.tactix4.t4openerp.connector.OERPResponse

/**
 * Implementation of the OpenERPOERPAdaptor and TransportDataConverter
 * to provide an XML-RPC transport Adaptor
 * @author max@tactix4.com
 *         7/12/13
 */
object XmlRpcOERPAdaptor extends OpenERPTransportAdaptor with XmlRpcResponses with Logging{



   implicit object XmlRpcDataTypeConverter extends TransportDataConverter[XmlRpcDataType]{

     def write(obj: XmlRpcDataType): OERPType = obj match{
       case x: XmlRpcInt     => OERPNumber(x.value)
       case x: XmlRpcDouble  => OERPNumber(x.value)
       case x: XmlRpcString  => OERPString(x.value)
       case x: XmlRpcBoolean => OERPBoolean(x.value)
       case x: XmlRpcBase64  => OERPString(x.value.toString)
       case x: XmlRpcArray   => OERPArray(x.value.map(write))
       case x: XmlRpcStruct  => OERPMap(x.value.map(x => x._1 -> x._2.toTransportDataType))
       case x: XmlRpcDate => OERPString(x.value.toString)
     }

     def read(obj: OERPType): XmlRpcDataType = obj match {
       case OERPNumber(n) => XmlRpcDouble(n)
       case OERPString(x) => XmlRpcString(x)
       case OERPBoolean(x) => XmlRpcBoolean(x)
       case OERPArray(x) => XmlRpcArray(x.map(y => read(y)))
       case OERPMap(x) => XmlRpcStruct(x.map(t => t._1 -> read(t._2)))
       case OERPNull => XmlRpcBoolean(value = false)
     }
   }



   override def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: List[OERPType]): Future[TransportResponse] = {

    val xmlRpcConfig = XmlRpcConfig(config.protocol, config.host, config.port, config.path, config.headers)
    val answer = XmlRpcClient.request(xmlRpcConfig, methodName, params.map(XmlRpcDataTypeConverter.read))
    val promise = Promise[XmlRpcResponse]()

    answer.onComplete(
      result => result match{
        case Success(r) => r.fold(
          (fault: XmlRpcClient.XmlRpcResponseFault) => ,
          (normal: XmlRpcClient.XmlRpcResponseNormal) => )
          case s: XmlRpcResponseNormal =>  promise.success(s.params.headOption.map(_.toOERPDataType).toRight("Empty response from server"))
          case s: XmlRpcResponseFault  =>  promise.success(Left(s.toString()))
        }
        case Failure(e) => promise.failure(e)
      }
    )
    promise.future
  }



 }
