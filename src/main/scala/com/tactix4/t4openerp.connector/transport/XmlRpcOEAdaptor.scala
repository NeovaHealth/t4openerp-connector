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
import com.tactix4.t4openerp.connector._

import com.typesafe.scalalogging.slf4j.Logging

import scalaz.syntax.id._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

/**
 * Implementation of the OpenERPOERPAdaptor and TransportDataConverter
 * to provide an XML-RPC transport Adaptor
 * @author max@tactix4.com
 *         7/12/13
 */
object XmlRpcOEAdaptor extends OETransportAdaptor with XmlRpcResponses with Logging{

  
  implicit def OpenERPTransportConfig2XmlRpcConfig(config:OETransportConfig):XmlRpcConfig =  XmlRpcConfig(config.protocol, config.host, config.port, config.path, config.headers)


   implicit object XmlRpcToOETypeConverter {

     def encode(obj: XmlRpcDataType): OEType = obj.fold(
      b => OEBoolean(b),
      i => OENumber(i),
      d => OENumber(d),
      d => OEString(d.toString),
      b => OEString(new String(b)),
      s => OEString(s),
      a => OEArray(a.map(encode)),
      s => OEMap(s.mapValues(encode))
     )

     def decode(obj: OEType): XmlRpcDataType = obj.fold(
       b => XmlRpcBoolean(b),
       d => if(d.isValidInt)XmlRpcInt(d.intValue()) else XmlRpcDouble(d.doubleValue()),
       s => XmlRpcString(s),
       a => XmlRpcArray(a.map(decode)),
       m => XmlRpcStruct(m.mapValues(decode)),
       _ => XmlRpcBoolean(false))

   }


   override def sendRequest(config: OETransportConfig, methodName: String, params: List[OEType]): FutureResponse[ErrorMessage,OEType] = {

     val ps = params.map(XmlRpcToOETypeConverter.decode)

    val request = XmlRpcClient.request(OpenERPTransportConfig2XmlRpcConfig(config), methodName, ps)

    request.map(_.fold(
       (fault: XmlRpcClient.XmlRpcResponseFault) => fault.toString().left,

       (normal: XmlRpcClient.XmlRpcResponseNormal) => {
          val array = for {
            h <- normal.params.headOption
          } yield XmlRpcToOETypeConverter.encode(h)

         array.fold(s"Unexpected result from OpenERP Server: $normal".left[OEType])(_.right[String])
       })) recover{
      case e: Throwable => e.getMessage.left
    }
  }

                       

 }
