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
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Try, Success, Failure}
import ExecutionContext.Implicits.global
import scala.language.implicitConversions
import com.tactix4.t4openerp.connector.exception.{OpenERPAuthenticationException, OpenERPException}

/**
 * Entry point into library and used to create an OpenERPSession
 * @param protocol the protocol with which to connect
 * @param host the openerp host to connect to
 * @param port the port on whcih the openerp host is listening for rpc requests
 * @param numWorkers the number of workers/threads for the connector to use - defaults to 5
 *
 * @author max@tactix4.com
 *         14/07/2013
 */
class OpenERPConnector(protocol: String, host: String, port: Int, numWorkers: Int = 5) extends Logging {


  val config = OpenERPTransportAdaptorConfig(protocol, host, port, RPCService.RPC_COMMON.toString)
  val transportClient:OpenERPTransportAdaptor = XmlRpcTransportAdaptor


  def getDatabaseList:Future[List[String]] ={
    config.path = RPCService.RPC_DATABASE.toString

    val promise = Promise[List[String]]()
    transportClient.sendRequest(config, "list",Nil).onComplete({
      case Success(s) => s.fold(
        (error: String) => promise.failure(new OpenERPException(error)),
        (result: TransportDataType) => result.value match {
          case x:List[_] => promise.success(x.map(_.toString))
          case fail => promise.failure(new OpenERPException("unexpected response in getDatabaseList: " + fail))
        })
      case Failure(f) => promise.failure(f)
    })

    promise.future
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
  def startSession(username: String, password: String, database: String) : Future[OpenERPSession]= {
    config.path = RPCService.RPC_COMMON.toString

    val result = Promise[OpenERPSession]()

    val answer = transportClient.sendRequest(config, "login", database, username, password)

    answer.onComplete {
      _ match {
        case Failure(f)    => { logger.error("Login attempt has failed: " + f.getMessage); result.failure(f)}
        case Success(f)    => f.fold(
          (error:String) => result.failure(new OpenERPException(error)),
          (v: TransportDataType) => v match {
            case _:TransportBoolean => result.failure( new OpenERPAuthenticationException("login failed with username: " + username + " and password: " + password))
            case TransportNumber(i:Int) => {
              val session = new OpenERPSession(transportClient,config,database,i, password)
              session.getContextFromServer.onComplete(_ match{
                case Success(s) => result.complete(Try(session))
                case Failure(fail) => result.failure(fail)
              })
            }
            case _ => result.failure(new OpenERPException("Unexpected response from server: "  + v))
          })
      }
    }

    result.future
  }


}

object RPCService extends Enumeration {
  implicit def RPCServiceToString(service: RPCService.Value) = service.toString

  //the three possible xml rpc paths to hit
  val RPC_COMMON = Value("/xmlrpc/common")
  val RPC_OBJECT = Value("/xmlrpc/object")
  val RPC_DATABASE = Value("/xmlrpc/db")

}
