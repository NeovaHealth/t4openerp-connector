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

package com.tactix4.t4openerp.connector

import com.tactix4.t4openerp.connector.transport._

import com.typesafe.scalalogging.slf4j.Logging

import scalaz._
import Scalaz._

import scala.language.implicitConversions

/**
 * Entry point into library and used to create an OpenERPSession
 * @param protocol the protocol with which to connect
 * @param host the openerp host to connect to
 * @param port the port on whcih the openerp host is listening for rpc requests
 *
 * @author max@tactix4.com
 *         14/07/2013
 */
class OEConnector(protocol: String, host: String, port: Int) extends Logging {


  val config = OETransportConfig(protocol, host, port, RPCService.RPC_OBJECT.toString)
  val transportClient:OETransportAdaptor = XmlRpcOEAdaptor


  def getDatabaseList:OEResult[List[String]] ={
    val conf = config.copy(path = RPCService.RPC_DATABASE.toString)

    transportClient.sendRequest(conf, "list",Nil).ffMap( result =>  {
      val dbList = result.asArray(_.map(_.string).flatten.success[ErrorMessage])
      dbList |  s"Unexpected result when querying database list: $result".failure[List[String]]
    })
  }

  /**
   * Begin a session with openerp by logging in a receiving a userid which is to be used in
   * subsequent requests
   *
   * OpenERP responds as follows:
   *  if the method name (in this case 'login') or database name are incorrect we get a fault back
   *  if the username or password is incorrect we receive a boolean 'false'
   *  if we successfully login, we receive a userid
   *
   * @param username the username to use to login to OpenERP
   * @param password the password to use to login to OpenERP
   * @param database the database to connect to
   * @return a Future[OpenERPSession] which on success represents the session of the logged in user.
   */
  def startSession(username: String, password: String, database: String,context: Option[OEContext] = None) : OESession= {
    val conf = config.copy(path = RPCService.RPC_COMMON.toString)

    val uid = transportClient.sendRequest(conf, "login", database, username, password).fold(
          (error:String) => error.failure[Int])(
          (v: OEType) =>  v.int.fold(s"Login failed: $v".failure[Int])(_.success[ErrorMessage])
      )

    OESession(uid,transportClient,config,database,password,context|OEContext())
  }
}

object RPCService extends Enumeration {
  implicit def RPCServiceToString(service: RPCService.Value) = service.toString

  //the three possible xml rpc paths to hit
  val RPC_COMMON = Value("/xmlrpc/common")
  val RPC_OBJECT = Value("/xmlrpc/object")
  val RPC_DATABASE = Value("/xmlrpc/db")

}
