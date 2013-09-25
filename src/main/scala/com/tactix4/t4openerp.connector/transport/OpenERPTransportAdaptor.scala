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

import scala.concurrent.Future
import com.tactix4.t4openerp.connector._

/**
 * Adaptor to be implemented for each transport mechanism supported
 * currently only XML-RPC but soon to be JSON-RPC
 *
 *  @author max@tactix4.com
 *         7/10/13
 *
 */
trait OpenERPTransportAdaptor {
  /**
   * send an RPC request over the implementing protocol
   * @param config the [[com.tactix4.t4openerp.connector.transport.OpenERPTransportAdaptorConfig]] to use
   * @param methodName the remote method to call
   * @param params a list of parameters to supply
   * @return A Future[TransportResponse] containing the response from the server
   */
  def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: List[TransportDataType]) : Future[TransportResponse]
  /**
   * send an RPC request over the implementing protocol
   * @param config the [[com.tactix4.t4openerp.connector.transport.OpenERPTransportAdaptorConfig]] to use
   * @param methodName the remote method to call
   * @param params the parameters to supply
   * @return A Future[TransportResponse] containing the response from the server
   */
  def sendRequest(config: OpenERPTransportAdaptorConfig, methodName: String, params: TransportDataType*) : Future[TransportResponse]= {
    sendRequest(config, methodName,params.toList)
  }
}

/**
 * Class to represent the transport adaptors config
 * @param protocol which protocol to use HTTP/HTTPS
 * @param host the host to connect to
 * @param port the port to use
 * @param path the path of the host to POST to
 * @param headers optional headers to attach the the outgoing requests
 */
case class OpenERPTransportAdaptorConfig(protocol: String, host: String, port: Int, var path: String, headers: Map[String, String] = Map())

