package com.tactix4.openerpConnector

import com.tactix4.openerpConnector.transport._
import com.typesafe.scalalogging.log4j.Logging
import scala.concurrent.{Promise, Future}
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * @author max@tactix4.com
 *         14/07/2013
 */
class OpenERPProxy(protocol: String, host: String, port: Int) extends Logging {
  val config = OpenERPTransportAdaptorConfig(protocol, host, port, RPCService.RPC_COMMON.toString)
  val transportClient:OpenERPTransportAdaptor = XmlRpcTransportAdaptor

  /**
   * Begin a session with openerp by logging in a receiving a userid which is to be used in
   * subsequent requests
   *
   * OpenERP responds as follows:
   *  if the method name (in this case 'login') or database name are incorrect we get a fault back
   *  if the username or password is incorrect we receive a boolean 'false'
   *  if we successfully login, we receive a userid
   *
   * @param username
   * @param password
   * @param database
   * @return a Future[OpenERPSession] which on success represents the session of the logged in user.
   */
  def startSession(username: String, password: String, database: String) : Future[OpenERPSession]= {
    config.path = RPCService.RPC_COMMON.toString

    val result = Promise[OpenERPSession]()

    val answer = transportClient.sendRequest(config, "login", database, username, password)

    answer.onComplete {
      x => x match {
        case Failure(f)                  => { logger.error("Login attempt has failed: " + f.getMessage); result.failure(f)}
        case Success(f)    => {f.fold(
            (error: TransportString) => {logger.error(error.value); result.failure(new OpenERPException(error.value))},
            (v: List[TransportDataType]) => v.headOption.map( datatype => datatype match {
              case _:TransportBoolean => result.failure( new OpenERPAuthenticationException("login failed with username: " + username + " and password: " + password))
              case TransportNumber(i) => result.complete(Try(new OpenERPSession(transportClient,config,database,username, password,i.asInstanceOf[Int])))
              case unknown => result.failure(new OpenERPException("Unexpected response from server: " + unknown))
            }))
        }
      }
    }
    result.future
  }
}

object RPCService extends Enumeration {
  implicit def RPCServiceToString(service: RPCService.Value) = service.toString()


  //the three possible xml rpc paths to hit
  val RPC_COMMON = Value("/xmlrpc/common")
  val RPC_OBJECT = Value("/xmlrpc/object")
  val RPC_DATABASE = Value("/xmlrpc/db")

}