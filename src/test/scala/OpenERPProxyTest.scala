import java.lang.Exception
import org.scalatest.concurrent._
import org.scalatest.FunSuite
import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by max@tactix4.com
 * 5/20/13
 */
class OpenERPProxyTest extends FunSuite with Futures {


  test("list databases from openerp host") {
//    val result = OpenERPProxy.getDatabaseList(RPCProtocol.RPC_HTTP, "192.168.1.95", 8069)
//
//    result.onComplete {
//      case Success(x) => assert(true)
//      case Failure(x) => fail(x)
//    }
//
//    try {
//      Await.result(result, 5000 milli)
//    } catch {
//      case e: Exception => fail(e)
//    }
  }
}
