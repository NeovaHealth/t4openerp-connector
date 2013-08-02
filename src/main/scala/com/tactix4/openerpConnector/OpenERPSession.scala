package com.tactix4.openerpConnector

import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Promise, Future}
import com.tactix4.openerpConnector.transport._
import scala.util.{Try, Success, Failure}
import com.tactix4.openerpConnector.domain.Domain._
import ExecutionContext.Implicits.global
import com.tactix4.openerpConnector.field.FieldType
import scalaz.Memo._

import com.tactix4.openerpConnector.domain.Domain
/**
 * @author max@tactix4.com
 *         7/8/13
 */
class OpenERPSession(val transportAdaptor: OpenERPTransportAdaptor, val config: OpenERPTransportAdaptorConfig,val database:String, username: String,val password: String, val uid: Int) extends Logging {

  val modelAdaptors = immutableHashMapMemo[String, Future[OpenERPModelAdaptor]]{
   s: String => getModelAdaptor(s)
  }

  def search(model: String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[Int]]= {
    modelAdaptors(model).flatMap(_.search(domain, offset, limit, order))
  }

  def read(model: String, ids: List[Int], fieldNames: List[String] = Nil) : Future[List[List[(String, Any)]]] = {
    modelAdaptors(model).flatMap(_.read(ids, fieldNames))
  }

  def searchAndRead(model: String, domain: Option[Domain] = None, fields: List[String] = Nil, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[List[(String, Any)]]] = {
    modelAdaptors(model).flatMap(_.searchAndRead(domain, fields, offset, limit, order))
  }

  def create(model: String, fields: List[(String, TransportDataType)]) : Future[Int] = {
    modelAdaptors(model).flatMap(_.create(fields))
  }

  def write(model: String, ids: List[Int], fields: List[(String, TransportDataType)]) : Future[Boolean] = {
    modelAdaptors(model).flatMap(_.write(ids,fields))
  }

  def unlink(model:String, ids:List[Int]) : Future[Boolean] = {
    modelAdaptors(model).flatMap(_.unlink(ids))
  }

  def getModelAdaptor(model: String) : Future[OpenERPModelAdaptor] = {

    val promise = Promise[OpenERPModelAdaptor]()
    val domain: Domain = "model" === model
    transportAdaptor.sendRequest(config, "execute", database, uid, password, "ir.model", "search", domain.toTransportDataType).onComplete(
    (value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold(
      (error: String) => {logger.error(error); promise.failure(new OpenERPException(error))},
      (result: TransportDataType) => result match {
        case TransportArray(a) => {
          if(a.isEmpty) {
            promise.failure(new OpenERPException("No model with name: " + model + " exists"))
          } else {
            promise.success(new OpenERPModelAdaptor(model, this))
          }
        }
        case _ => promise.failure(new OpenERPException("No model with name: " + model + " exists"))
      }
      )
      case Failure(f) => promise.failure(new OpenERPException("Error creating model adaptor: " + f.getMessage,f))
    }
    )
    promise.future
  }



  //out biased defaults
  val context: OpenERPContext = new OpenERPContext(true,"en_GB","Europe/London");
  //TODO: is this broken????
  config.path = RPCService.RPC_OBJECT
  transportAdaptor.sendRequest(config,"execute",database, uid, password, "res.users", "context_get").onComplete((value: Try[TransportResponse]) => {
    value match {
      case Success(x) => x.fold(
        (error: String) => logger.error("Recieved a fault from openerp server: " + error),
        (result: TransportDataType) => result  match {
          case TransportMap(m) => {
             m.find(t => t._1.toString == "lang").map(v =>  context.setLanguage(v._2.toString))
             m.find(t => t._1.toString == "tz").map(v => context.setTimeZone(v._2.toString))
           }
          case unexpected => throw new OpenERPTransportException("unexpected response type: " + unexpected)
          })

      case Failure(f) => logger.error("Failed to get context from openERP server\n" + f.getMessage)

    }
  } )


}

object OpenERPSession{
  implicit def IntToListOfInts(i: Int) : List[Int] = List(i)
  implicit def StringToListOfStrings(s: String) : List[String] = List(s)
  implicit def TupleToListOfTuples (t : (String, TransportDataType)) : List[(String, TransportDataType)] = List(t)
}
