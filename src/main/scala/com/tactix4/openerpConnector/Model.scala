package com.tactix4.openerpConnector

import com.typesafe.scalalogging.log4j.Logging
import scala.concurrent.{ExecutionContext, Promise, Future}
import com.tactix4.openerpConnector.transport._
import scala.util.{Try, Success, Failure}
import com.tactix4.openerpConnector.domain.Domain
import ExecutionContext.Implicits.global
import com.tactix4.openerpConnector.field.{FieldType, Field}

/**
 * @author max@tactix4.com
 *         22/07/2013
 */


class OpenERPModelAdaptor(model: String, session: OpenERPSession) extends Logging {


  private val fields: Future[List[FieldType]] = getFields

  private def unexpected(e: String) = {
    new OpenERPException("Unexpected response: " + e)
  }

  private def getFields : Future[List[FieldType]] = {

    val promise = Promise[List[FieldType]]()

    session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "fields_get").onComplete(
      _ match {
        case Success(s) => s.fold(
          (error: String) => { logger.error(error); promise.failure(new OpenERPException(error)) },
          (result: TransportDataType) => result  match {
            case t: TransportMap => {

              //TODO: this is bad
              if(t.value.exists(_._2 match {
                case x: TransportMap => false
                case x  => {
                  println(x)
                  true
                }
              })) {
                println(t.toString)
                promise.failure(unexpected(t.toString))
              }
              else{
                val theFields = t.value.map(tuple => tuple._2 match {
                  case m: TransportMap => Field(tuple._1.value, m)
                })
                promise.success(theFields)
              }

            }
            case x => promise.failure(unexpected(x.toString))
          }
        )
      })

    promise.future
  }



  def getField(fieldName : String): Future[Option[FieldType]]= {

    val promise = Promise[Option[FieldType]]()
    val result = for {
      f <- fields
      v <- Future.successful(f.find(_.name == fieldName))
    } yield v

    result.map(
      _.map(field => promise.complete(Try(Some(field)))).getOrElse(promise.complete(Try(None)))
    )

    promise.future
  }

  def search(domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[Int]]= {

    session.config.path = RPCService.RPC_OBJECT
    val promise = Promise[List[Int]]()

    val result = for{
      obsIds <-  session.transportAdaptor.sendRequest(session.config,"execute", session.database, session.uid, session.password, model, "search", domain.map(_.toTransportDataType).getOrElse(""), offset, limit,order,session.context.toTransportDataType)
    } yield obsIds

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold(
        (error: String) => {logger.error(error); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result match {
            case TransportArray(a) => promise.complete(Try(a.map(_.toScalaType.asInstanceOf[Int])))
            case fail => promise.failure(new OpenERPException("Unexpected result: " + fail.toString))
          }
        )

      case Failure(f) => promise.failure(f)

    })

    promise.future

  }


  def read(ids: List[Int], fields: List[String] = Nil) : Future[List[List[(String, Any)]]] = {

    session.config.path = RPCService.RPC_OBJECT
    val promise = Promise[List[List[(String, Any)]]]()
    val result = for{
      values <-  session.transportAdaptor.sendRequest(session.config, "execute",session.database,session.uid,session.password,model,"read", ids, fields,session.context.toTransportDataType)
    } yield values

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: String) => {logger.error(error.value); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result match {
            case TransportArray(a:List[TransportMap]) => promise.complete(Try(a.map(_.toScalaType)))
            case fail => {logger.error("Unexpected type: " + fail.toString); promise.failure(new OpenERPException("unexpected type: " + fail))}
          }
        )

      case Failure(f) => promise.failure(f)
    })

    promise.future

  }

  def searchAndRead(domain: Option[Domain] = None, fields: List[String] = Nil, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[List[(String, Any)]]] = {

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[List[List[(String, Any)]]]()
    val result = for{
      obsIds <- search(domain, offset, limit, order)
      obsValues <- read(obsIds, fields)
    } yield obsValues

    result.onComplete((value: Try[List[List[(String, Any)]]]) => value match {
      case Success(s) => promise.complete(Try(s))
      case Failure(f) => promise.failure(f)
    } )

    promise.future
  }


  def create(fields: List[(String,String)]) : Future[Int] = {

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[Int]()
    val result = session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "create",TransportMap(fields.map(a=>TransportString(a._1) -> TransportString(a._2))), session.context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: String) => {logger.error(error); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result  match {
            case TransportNumber(i:Int) => promise.complete(Try(i))
            case fail => promise.failure(new OpenERPException(fail.toString))
          }
        )
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }


  def write(ids: List[Int], fields: List[FieldType]) : Future[Boolean] = {

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()
    val result = session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "write", ids,"" /*fields.toTransportDataType*/, session.context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold(
        (error: String) => { logger.error(error); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result  match{
            case TransportBoolean(b) => promise.complete(Try(b))
            case fail => promise.failure(new OpenERPException("Error reading result: " + result))
          })
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  def unlink(ids:List[Int]) : Future[Boolean]= {

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()
    val result = session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "unlink", ids, session.context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold((error: String) => {logger.error(error)},
        (result: TransportDataType) => result  match {
            case TransportBoolean(b) => promise.complete(Try(b))
            case fail => promise.failure(new OpenERPException("Error reading result: " + result))
          })
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }
}


