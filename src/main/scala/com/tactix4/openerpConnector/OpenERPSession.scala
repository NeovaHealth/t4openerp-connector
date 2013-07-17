package com.tactix4.openerpConnector

import com.typesafe.scalalogging.log4j.Logging
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import ExecutionContext.Implicits.global
import com.tactix4.openerpConnector.transport._
import com.tactix4.openerpConnector.domain.Domain

/**
 * @author max@tactix4.com
 *         7/8/13
 */
class OpenERPSession(val transportAdaptor: OpenERPTransportAdaptor, val config: OpenERPTransportAdaptorConfig,val database:String, username: String,val password: String, val uid: Int) extends Logging {


  //out biased defaults
  val context: OpenERPContext = new OpenERPContext(true,"en_GB","Europe/London");

  config.path = RPCService.RPC_OBJECT
  transportAdaptor.sendRequest(config,"execute",database, uid, password, "res.users", "context_get").onComplete((value: Try[TransportResponse]) => {
    value match {
      case Success(x) => x.fold(
        (error: TransportString) => logger.error("Recieved a fault from openerp server: " + error),
        (result: List[TransportDataType]) => result foreach(_ match {
           case TransportMap(m) => {
             m.find(t => t._1.toString == "lang").map(v =>  context.setLanguage(v._2.toString))
             m.find(t => t._1.toString == "tz").map(v => context.setTimeZone(v._2.toString))
           }
           case unexpected => throw new OpenERPTransportException("unexpected response type: " + unexpected)
        }))

      case Failure(f) => logger.error("Failed to get context from openERP server\n" + f.getMessage)

    }
  } )

  def search(model:String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[Int]]= {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[List[Int]]()
    val result = for{
      obsIds <-  transportAdaptor.sendRequest(config,"execute", database, uid, password, model, "search", domain.map(_.toTransportDataType).getOrElse(""), offset, limit,order,context.toTransportDataType)
    } yield obsIds

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold(
        (error: TransportString) => {logger.error(error.value); promise.failure(new OpenERPException(error.value))},
        (result: List[TransportDataType]) => result.headOption.map(
          _ match {
            case TransportArray(a) => promise.complete(Try(a.map(_.toScalaType.asInstanceOf[Int])))
            case fail => promise.failure(new OpenERPException("Unexpected result: " + fail.toString))
          }
        ).getOrElse(promise.failure(new OpenERPException("Empty result found"))))

      case Failure(f) => promise.failure(f)

    })

    promise.future

  }


  def read(model:String, ids: List[Int], fields: List[String] = Nil) : Future[List[List[(String, Any)]]] = {

    config.path = RPCService.RPC_OBJECT
    val promise = Promise[List[List[(String, Any)]]]()
    val result = for{
      values <-  transportAdaptor.sendRequest(config, "execute",database,uid,password,model,"read", ids, fields,context.toTransportDataType)
    } yield values

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: TransportString) => {logger.error(error.value); promise.failure(new OpenERPException(error.value))},
        (result: List[TransportDataType]) => result.headOption.map(
          _ match {
            case TransportArray(a:List[TransportMap]) => promise.complete(Try(a.map(_.toScalaType)))
            case fail => {logger.error("Unexpected type: " + fail.toString); promise.failure(new OpenERPException("unexpected type: " + fail))}
          }
        ).getOrElse(promise.failure(new OpenERPException("Empty result found"))))

      case Failure(f) => promise.failure(f)
    })

    promise.future

  }

  /**  Does a search and read operation
	 * @param model The model name over which to search
	 * @param domain An optional domain to restrict the search
	 * @param offset Number of records to skip. -1 for no offset.
	 * @param limit Maximum number of rows to return. -1 for no limit.
	 * @param order Field name to order on
	 * @return A list of list of tuples representing the result of the search
 */


  def searchAndRead(model:String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[List[(String, Any)]]] = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[List[List[(String, Any)]]]()
    val result = for{
      obsIds <- search(model, domain, offset, limit, order)
      obsValues <- read(model, obsIds)
    } yield obsValues

    result.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => promise.complete(Try(s))
      case Failure(f) => promise.failure(f)
    } )

    promise.future
  }


  def create(model:String, fields: TransportMap) : Future[Int] = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[Int]()
    val result = transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "create", fields, context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: TransportString) => {logger.error(error.value); promise.failure(new OpenERPException(error.value))},
        (result: List[TransportDataType]) => result.headOption.map(
          _ match {
            case TransportNumber(i:Int) => promise.complete(Try(i))
            case fail => promise.failure(new OpenERPException(fail.toString))
          }).getOrElse(
            promise.failure(new OpenERPException("Error reading result: " + result))
        )
      )
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }


  def update(model:String, ids: List[Int], fields: TransportMap) : Future[Boolean] = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()
    val result = transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "write", ids, fields, context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold(
        (error: TransportString) => { logger.error(error.value); promise.failure(new OpenERPException(error.value))},
        (result: List[TransportDataType]) => result.headOption.map(
          _ match{
            case TransportBoolean(b) => promise.complete(Try(b))
            case fail => promise.failure(new OpenERPException("Error reading result: " + result))
          }).getOrElse(promise.failure(new OpenERPException("Error reading result: " + result)))
      )
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  def delete(model:String, ids:List[Int]) = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[TransportResponse]()
    val result = transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "unlink", ids, context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => promise.complete(value))

    promise.future
  }

}
