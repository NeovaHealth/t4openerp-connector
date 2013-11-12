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
import com.tactix4.t4openerp.connector.TransportResponse

/**
 * Implementation of the OpenERPTransportAdaptor and TransportDataConverter
 * to provide an XML-RPC transport Adaptor
 * @author max@tactix4.com
 *         7/12/13
 */
object XmlRpcTransportAdaptor extends OpenERPTransportAdaptor with Logging{



   implicit object XmlRpcDataTypeConverter extends TransportDataConverter[XmlRpcDataValue]{

     def write(obj: XmlRpcDataValue): TransportDataType = obj match{
       case x: XmlRpcInt     => TransportNumber(x.value)
       case x: XmlRpcDouble  => TransportNumber(x.value)
       case x: XmlRpcString  => TransportString(x.value)
       case x: XmlRpcBoolean => TransportBoolean(x.value)
       case x: XmlRpcBase64  => TransportString(x.value.toString)
       case x: XmlRpcArray   => TransportArray(x.value.map(write))
       case x: XmlRpcStruct  => TransportMap(x.value.map(x => x._1 -> x._2.toTransportDataType))
       case x: XmlRpcDateTime => TransportString(x.value.toString)
     }

     def read(obj: TransportDataType): XmlRpcDataValue = obj match {
       case TransportNumber(x:Int) => XmlRpcInt(x)
       case TransportNumber(x:Double) => XmlRpcDouble(x)
       case TransportNumber(x:Float) => XmlRpcDouble(x.toDouble)
       case TransportString(x) => XmlRpcString(x)
       case TransportBoolean(x) => XmlRpcBoolean(x)
       case TransportArray(x) => XmlRpcArrayType(x.map(y => read(y)))
       case TransportMap(x) => XmlRpcStructType(x.map(t => t._1 -> read(t._2)))
       case TransportNull => XmlRpcBoolean(value = false)
     }
   }



   override def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: List[TransportDataType]): Future[TransportResponse] = {
    logger.debug("sending request with\nconfig: " + config + "\nmethodName: " + methodName + "\nparams: " + params.map(_.value))

    val xmlRpcConfig = XmlRpcConfig(RPCProtocol.stringToRpcProtocol(config.protocol), config.host, config.port, config.path, config.headers)
    val answer = XmlRpcClient.request(xmlRpcConfig, methodName, params.map(x => XmlRpcDataTypeConverter.read(x)))
    val promise = Promise[TransportResponse]()

    answer.onComplete(
      result => result match{
        case Success(r) => r match {
          case s: XmlRpcResponseNormal =>  promise.complete(Try(s.params.headOption.map(_.toTransportDataType).toRight("Empty response from server")))
          case s: XmlRpcResponseFault  =>  promise.complete(Try(Left(s.toString())))
        }
        case Failure(e) => promise.failure(e)
      }
    )
    promise.future
  }



 }
