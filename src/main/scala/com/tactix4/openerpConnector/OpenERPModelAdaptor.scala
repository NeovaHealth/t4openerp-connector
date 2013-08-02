package com.tactix4.openerpConnector
import com.typesafe.scalalogging.slf4j.Logging
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


  private lazy val fields: Future[List[FieldType]] = getFields

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
                val theFields = t.value.collect {
                  case (x, y: TransportMap) => Field(x.value, y)
                }
                if(theFields.size != t.value.size){
                  throw new OpenERPException("Problem parsing openERP get_field response.\nExpected maps of maps: " + t)
                }
                else  promise.success(theFields)
            }
            case x => promise.failure(unexpected(x.toString))
          }
        )
      case Failure(f) => promise.failure(f)
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

  def getFieldType(fieldName: String): Future[Option[String]] = {
    getField(fieldName).map(_.map(_.fieldType))
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


  def create(fields: List[(String,TransportDataType)]) : Future[Int] = {

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[Int]()
    val result = session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "create",TransportMap(fields.map(a=>TransportString(a._1) -> a._2)), session.context.toTransportDataType)

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


  def write(ids: List[Int], fields: List[(String, TransportDataType)]) : Future[Boolean] = {

    val processedFields: Future[List[(TransportString, TransportDataType)]] = Future.sequence(fields.map(
      field => getField(field._1).map(
        _.map((fieldT: FieldType) => fieldT.fieldType match{

          //we're simplifying the write method for many2many and simply providing the 'replace' option
          case "many2many" => {
            /*
             * valid args are an array of ints
             */
            val validM2MArgs : PartialFunction[TransportDataType,Boolean] = {
              case x: TransportArray => x.value.forall(_ match {
                case _:TransportNumber[Int] => true
                case _ => false
              })
            }
            if(validM2MArgs.isDefinedAt(field._2)){
              TransportString(field._1) -> TransportArray(List(TransportArray(List(6,0,field._2))))
            }
            else {
              logger.error("Invalid arguments to write to a many2many field. Expected a lit of Ints, recieved: " + field._2.toScalaType)
              throw new OpenERPException("Invalid arguments to write to a many2many field. Expected a list of Ints, recieved: " + field._2.toScalaType)
            }


          }
          //case "one2many" => // not sure about the best way to do this as there isn't an equivelent to the many2many option
         case "many2one" => {
           /*
            * valid args are an int or false
            */
           val validM2OArgs : PartialFunction[TransportDataType, Boolean] = {
             case x: TransportNumber[Int] => true
             case x: TransportBoolean if !x.value => true
           }
           if(validM2OArgs.isDefinedAt(field._2)){
             TransportString(field._1) -> field._2
           }
           else {
             logger.error("Invalid arguments to write to a many2one field. Expected an Int or false, recieved: " + field._2.toScalaType)
             throw new OpenERPException("Invalid arguments to write to a many2one field. Expected an Int or false, recieved: " + field._2.toScalaType)
           }
         }
          //you know that? it's faster if I just let openerp return a fault for invalid args....

//          case "selection" => {
//            fieldT.parameters.value.headOption.map(h =>
//              if(h._1.value == "selection"){
//                h._2 match {
//                  case x: TransportArray => {
//                    val strings = x.value collect {
//                      case y: TransportArray => y.value collect {
//                        case z: TransportString => z.value
//                      }
//                    }
//                    val r = strings.flatten.find(_ == field._2.toScalaType)
//                    r.map(_ => TransportString(field._1) -> field._2).getOrElse(
//                      throw new OpenERPException("Invalid argument to selection field. " + field._2.toScalaType + " was not found in valid selection list")
//                    )
//                  }
//                  case _ => throw new OpenERPException("Error evaluating selection field: " + fieldT)
//
//                }
//              }
//              else{ throw new OpenERPException("Error evaluating selection field: " + fieldT)}
//            ).getOrElse(
//              throw new OpenERPException("Invalid argument to selection field. " + field._2.toScalaType + " was not found in valid selection list")
//            )
//
//
//          }


          case _ => TransportString(field._1) -> field._2
        }).getOrElse(throw new OpenERPException("Field " + field +  " doesn't exist?"))
      )
    ))

    session.config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()

    val result = for {
      f <- processedFields
      r <- session.transportAdaptor.sendRequest(session.config, "execute", session.database, session.uid, session.password, model, "write", ids,TransportMap(f), session.context.toTransportDataType)

    } yield r

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


