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

import com.tactix4.t4openerp.connector._

import com.typesafe.scalalogging.slf4j.{LazyLogging}
import scalaz.syntax.either._
import scala.language.implicitConversions
import com.tactix4.t4xmlrpc._
import java.util.concurrent.Executor

/**
 * Implementation of the OpenERPOERPAdaptor and TransportDataConverter
 * to provide an XML-RPC transport Adaptor
 * @author max@tactix4.com
 *         7/12/13
 */
class XmlRpcOEAdaptor(ex:Option[Executor]) extends OETransportAdaptor with LazyLogging{

  val client = new XmlRpcClient(ex)
  
  implicit def OpenERPTransportConfig2XmlRpcConfig(config:OETransportConfig):XmlRpcConfig =  XmlRpcConfig(config.protocol, config.host, config.port, config.path, config.headers)


   implicit object XmlRpcToOE {

     def encode(obj: XmlRpcDataType): OEType = obj.fold(
      b => OEBoolean(b),
      i => OENumber(i),
      d => OENumber(d),
      d => OEString(d.toString),
      b => OEString(new String(b)),
      s => OEString(s),
      a => OEArray(a.map(encode)),
      s => OEDictionary(s.mapValues(encode))
     )

     def decode(obj: OEType): XmlRpcDataType = obj.fold(
       b => XmlRpcBoolean(b),
       d => if(d.scale == 0)XmlRpcInt(d.intValue()) else XmlRpcDouble(d.doubleValue()),
       s => XmlRpcString(s),
       a => XmlRpcArray(a.map(decode)),
       m => XmlRpcStruct(m.mapValues(decode)),
       _ => XmlRpcBoolean(false))

   }


   override def sendRequest(config: OETransportConfig, methodName: String, params: List[OEType]): FutureEither[OEType] = {

     client.request(OpenERPTransportConfig2XmlRpcConfig(config), methodName, params.map(XmlRpcToOE.decode)).fold(
       (f: XmlRpcResponseFault) => f.toString.left[OEType],
       (n: XmlRpcResponseNormal) => n.params.fold(
         (error: ErrorMessage) => error.left[OEType],
         (d: List[XmlRpcDataType]) => OEArray(d.map(XmlRpcToOE.encode)).right[ErrorMessage]
       ))
   }

 }
