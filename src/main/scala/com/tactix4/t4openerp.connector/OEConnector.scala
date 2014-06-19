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


import scala.language.implicitConversions
import scala.concurrent.Future
import com.typesafe.scalalogging.slf4j.LazyLogging
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

/**
 * Entry point into library and used to create an OpenERPSession
 * @param protocol the protocol with which to connect
 * @param host the openerp host to connect to
 * @param port the port on which the openerp host is listening for rpc requests
 *
 * @author max@tactix4.com
 *         14/07/2013
 */
class OEConnector(protocol: String, host: String, port: Int, ex:Option[Executor] = None) extends LazyLogging {



  val config = OETransportConfig(protocol, host, port, RPCService.RPC_OBJECT.toString)
  val transportClient:OETransportAdaptor = new XmlRpcOEAdaptor(ex)


  def getDatabaseList:EitherT[Future,ErrorMessage,List[String]] ={
    val conf = config.copy(path = RPCService.RPC_DATABASE.toString)

    for {
      t <- transportClient.sendRequest(conf, "list",Nil)
      a <- (t.array \/> "Response was not an array").asET
      s <- (a.map(_.string).sequence[Option, String] \/> "Response was not an array of strings").asET
    } yield s

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
   * @return an OESession
   */
  def startSession(username: String, password: String, database: String,context: Option[OEContext] = None) : OESession= {
    val conf = config.copy(path = RPCService.RPC_COMMON.toString)

    val uid = transportClient.sendRequest(conf, "login", database, username, password).flatMap(
      _.int \/> "Login failed" asET
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
