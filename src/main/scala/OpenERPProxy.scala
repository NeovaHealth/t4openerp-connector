/**
 * Created by max@tactix4.com
 * @ 5/20/13
 */

import com.tactix4.simpleXmlRpc._
import com.tactix4.simpleXmlRpc.XmlRpcPreamble._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.util.{Try, Failure, Success}


object RPCService extends Enumeration {
  implicit def RPCServiceToString(service: RPCService.Value) = service.toString()


  //the three possible xml rpc paths to hit
  val RPC_COMMON = Value("/xmlrpc/common")
  val RPC_OBJECT = Value("/xmlrpc/object")
  val RPC_DATABASE = Value("/xmlrpc/db")
}

class OpenERPProxy(protocol: RPCProtocol.Value, host: String, port: Int) {
  var userID = 0
  val config = XmlRpcConfig(protocol, host, port, RPCService.RPC_COMMON)


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
   * @return a Future[Int] which on success represents the userid of the logged in user.
   */
  def startSession(username: String, password: String, database: String) : Future[Int]= {
    config.path = RPCService.RPC_COMMON

    val result = Promise[Int]

    val answer = XmlRpcClient.request(config, "login", List(
      ("username", XmlRpcString(username)),
      ("password", XmlRpcString(password)),
      ("database", XmlRpcString(database))))

    answer.onComplete {
      x => x match {
        case Failure(f)                       => result.failure(f)
        case Success(s: XmlRpcResponseFault)  => result.failure(new OpenERPXMLRPCException(s.faultCode + "\n" + s.faultString))
        case Success(s: XmlRpcResponseNormal) => s.params.head match {
          case b: XmlRpcBoolean => result.failure( new OpenERPAuthenticationException("login failed with username: " + username + " and password: " + password))
          case i: XmlRpcInt     => { userID = i.value; result.complete(Try(i.value))}
        }
      }
    }
    result.future
  }
}


