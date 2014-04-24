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

/**
 * Adaptor to be implemented for each transport mechanism supported
 *  @author max@tactix4.com
 *         7/10/13
 */
trait OETransportAdaptor {
  /**
   * send an RPC request over the implementing protocol
   * @param config the [[com.tactix4.t4openerp.connector.transport.OETransportConfig]] to use
   * @param methodName the remote method to call
   * @param params a list of parameters to supply
   * @return A OEResult[OEType] containing the response from the server
   */
  def sendRequest(config: OETransportConfig, methodName: String, params: List[OEType]) : OEResult[OEType]
  /**
   * send an RPC request over the implementing protocol
   * @param config the [[com.tactix4.t4openerp.connector.transport.OETransportConfig]] to use
   * @param methodName the remote method to call
   * @param params the parameters to supply
   * @return A OEResult[OEType] containing the response from the server
   */
  def sendRequest(config: OETransportConfig, methodName: String, params: OEType*) : OEResult[OEType] = {
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
case class OETransportConfig(protocol: String, host: String, port: Int, path: String, headers: Map[String, String] = Map())

