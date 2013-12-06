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

import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Try, Success, Failure}
import scalaz.Memo._
import ExecutionContext.Implicits.global
import scala.language.implicitConversions
import com.tactix4.t4openerp.connector.domain.Domain
import transport._
import com.tactix4.t4openerp.connector.exception.OpenERPException
import com.tactix4.t4openerp.connector.field.Field
import java.util.Date

/**
 * An OpenERPSession represents a current session to an OpenERP server.
 *
 * It is the main class that a user will interact with - allowing access to common ORM methods
 *
 * @example {{{
 *           val session = new OpenERPConnector("http", openerpHost,openerpPort).startSession(username,password,database)
 *
 *           val result = for {
 *              s <- session
 *              id <- s.read('res.users', 1)
 *           } yield id
 *
 *           result.onComplete((value: Try[ResultType]) => value match {
 *              case Success(s) => s.foreach(_ foreach println)
 *              case Failure(f) => println(f)
 *           })
 *
 * }}}
 *
 * @see https://doc.openerp.com/trunk/server/api_models/ for details of the methods
 *
 * @param transportAdaptor the transportAdaptor through which to communicate to the OpenERP server
 * @param config the transport adaptor config
 * @param database the database with which we are communicating
 * @param uid the userid we are using to connect
 * @param password the password we are using to connect
 *
 * @author max@tactix4.com
 *         7/8/13
 */
class OpenERPSession(val transportAdaptor: OpenERPTransportAdaptor, val config: OpenERPTransportAdaptorConfig,val database:String, val uid: Int,val password: String) extends Logging {


  val context: OpenERPContext = new OpenERPContext(true,"en_GB","Europe/London")

  /**
   * A caching hashmap for the model fields
   */
  val getFields = immutableHashMapMemo[String, Future[List[Field]]]{
   s: String => getFieldsFromModel(s)
  }

  /**
   * convenience method to log error and return an [[com.tactix4.t4openerp.connector.exception.OpenERPException]]
   * @param e the error string
   * @return an [[com.tactix4.t4openerp.connector.exception.OpenERPException]]
   */
  private[OpenERPSession] def unexpected(e: String) = {
    logger.error("Unexpected response: " + e)
    new OpenERPException("Unexpected response: " + e)
  }

  /**
   * Fetch the fields for this model
   *
   * @return a [[scala.concurrent.Future]] containing a [[scala.collection.immutable.List]] of [[com.tactix4.t4openerp.connector.field.Field]]s
   * @throws OpenERPException if the response from the server does not meet our expectations - or if we receive an exception from the [[com.tactix4.t4openerp.connector.transport.OpenERPTransportAdaptor]]
   */
  private[OpenERPSession] def getFieldsFromModel(model: String): Future[List[Field]] = {

    val promise = Promise[List[Field]]()

    transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "fields_get", TransportArray(Nil), context.toTransportDataType).onComplete(
      _ match {
        case Success(s) =>
          s.fold(
          (error: String) => { logger.error(error); promise.failure(new OpenERPException(error)) },
          (result: TransportDataType) => result  match {
            case t: TransportMap => {
                val theFields = t.value.collect { case (x, y: TransportMap) => new Field(x.value, y) }
                if(theFields.size != t.value.size) throw unexpected(t.toString)
                else promise.success(theFields.toList)
            }
            case x => promise.failure(unexpected(x.toString))
          }
        )
      case Failure(f) => promise.failure(f)
      })

    promise.future
  }

  /**
   * Fetch the context from the server and populate our [[com.tactix4.t4openerp.connector.OpenERPContext]] instance
   * @return true indicating success
   * @throws OpenERPException if the response was not expected
   */
   def getContextFromServer : Future[Boolean] = {
    val promise = Promise[Boolean]()
      config.path = RPCService.RPC_OBJECT
      val answer = transportAdaptor.sendRequest(config,"execute",TransportString(database), TransportNumber(uid), TransportString(password), TransportString("res.users"), TransportString("context_get"))
      answer.onComplete((value: Try[TransportResponse]) => {
        value match {
          case Success(x) => x.fold(
            (error: String) => {logger.error("Recieved a fault from openerp server: " + error); promise.failure(new OpenERPException(error))},
            (result: TransportDataType) => result  match {
              case TransportMap(m) => {
                m.find(t => t._1 == "lang").map(v =>  context.setLanguage(v._2.toString))
                m.find(t => t._1 == "tz").map(v => context.setTimeZone(v._2.toString))
                promise.complete(Try(true))
              }
              case unexpected => promise.failure(new OpenERPException("unexpected response type: " + unexpected))
            })

          case Failure(f) => {logger.error("Failed to get context from openERP server\n" + f.getMessage); promise.failure(f)}

        }
      } )
    promise.future
  }



  /**
   * Get the named Field, if it exists
   *
   * @param model the model to which the field belongs
   * @param fieldName the name of the field you want to retrieve
   * @return a [[scala.concurrent.Future]] containing an [[scala.Option]] of a [[com.tactix4.t4openerp.connector.field.Field]]
   */
  def getField(model: String, fieldName : String): Future[Option[Field]]= {
      getFields(model).map(_.find(_.name == fieldName))
  }

  /**
   * Get the type of the Field, if it exists
   *
   * @param model the model to which the field belongs
   * @param fieldName the name of the field that you want to know the type of
   * @return a [[scala.concurrent.Future]] containing an [[scala.Option]] of a [[java.lang.String]] representing the type of the field
   */
  def getFieldType(model: String, fieldName: String): Future[Option[String]] = {
    getField(model, fieldName).map(_.flatMap(_.get("type")))
  }

  /**
   * Searches the openerp server with the following parameters
   *
   * @param domain an [[scala.Option]] contain a [[com.tactix4.t4openerp.connector.domain.Domain]] object - default is None
   * @param offset an [[scala.Int]] indicating the number of records to skip - default is 0
   * @param limit an [[scala.Int]] indicating the maximum number of records to return - default is 0
   * @param order a [[java.lang.String]] indicating the column name by which to sort the records - server default is "id"
   * @return a [[scala.concurrent.Future]] containing a [[scala.collection.immutable.List]] of type [[scala.Int]] representing the ids of the matching records
   * @throws OpenERPException if the response from the server does not meet our expectations - or if we receive an exception from the [[com.tactix4.t4openerp.connector.transport.OpenERPTransportAdaptor]]
   */
  def search(model: String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = "") : Future[List[Int]]= {
    config.path = RPCService.RPC_OBJECT
    val promise = Promise[List[Int]]()

    val result = for{
      obsIds <-  transportAdaptor.sendRequest(config,"execute", database, uid, password, model, "search", domain.map(_.toTransportDataType).getOrElse(""), offset, limit,order,context.toTransportDataType)
    } yield obsIds

    result.onComplete({
      case Success(s) => s.fold(
        (error: String) => promise.failure(new OpenERPException(error)),
        (result: TransportDataType) => result match {
            case TransportArray(a) => promise.complete(Try(a.map( {
              case TransportNumber(y:Int) => y
              case x => throw unexpected(x.toString)
            })))
            case fail => promise.failure(unexpected(fail.toString))
          }
        )

      case Failure(f) => promise.failure(f)

    })

    promise.future
  }

 /**
   * Read/fetch the supplied ids from the openERP server
   *
   * @param ids a [[scala.collection.immutable.List]] of type [[scala.Int]] representing the ids of the records to read
   * @param fieldNames a [[scala.collection.immutable.List[String]]]
   * @return
   * @throws OpenERPException if the response from the server does not meet our expectations - or if we receive an exception from the [[com.tactix4.t4openerp.connector.transport.OpenERPTransportAdaptor]]
   */
 def read(model: String, ids: List[Int], fieldNames: List[String] = Nil) : Future[ResultType] = {

    config.path = RPCService.RPC_OBJECT
    val promise = Promise[ResultType]()
    val result = for{
      values <-  transportAdaptor.sendRequest(config, "execute",database,uid,password,model,"read", ids.toTransportDataType, fieldNames.toTransportDataType,context.toTransportDataType)
    } yield values

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: String) => promise.failure(new OpenERPException(error)),
        (result: TransportDataType) => result match {
            case TransportArray(x) => promise.complete(Try(x.map({
              case y:TransportMap => y.value
              case fail => throw unexpected(fail.toString)
            })))
            case fail => promise.failure(unexpected(fail.toString))

          }
        )

      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  /**
   * Combines the search and the read operations for convenience
   * @param model the model to query
   * @param domain the optional domain to filter the query
   * @param fields the fields to return
   * @param offset the number of records to skip
   * @param limit the maximum number of records to return
   * @param order the column name by which to sort the results
   * @return a [[scala.concurrent.Future]] containing the results of the query
   */
  def searchAndRead(model: String, domain: Option[Domain] = None, fields: List[String] = Nil, offset: Int = 0, limit: Int = 0, order: String = "") : Future[ResultType] = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[ResultType]()

    val result = for{
      obsIds <- search(model,domain, offset, limit, order)
      obsValues <- read(model,obsIds, fields)
    } yield obsValues

    result.onComplete({
      case Success(s) => promise.complete(Try(s))
      case Failure(f) => promise.failure(f)
    } )

    promise.future
  }

  /**
   * Create a new record
   * @param model the model within which to create the record
   * @param fields the fieldnames and values to write
   * @return a Future containing the id of the new record
   * @throws OpenERPException if the creation fails
   */
  def create[T:TransportDataConverter](model: String, fields: Map[String, T]) : Future[Int] = {
     logger.info("executing create with fields: " + fields)

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[Int]()
    val result = transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "create",TransportMap(fields.mapValues(anyToTDT)), context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match{
      case Success(s) => s.fold(
        (error: String) => promise.failure(new OpenERPException(error)),
        (result: TransportDataType) => result  match {
            case TransportNumber(i:Int) => promise.complete(Try(i))
            case fail => promise.failure(unexpected(fail.toString))
          }
        )
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  /**
   * write to and update an existing record
   *
   * Depending on the type of the record to update - different field values are expected.
   * Refer to the openerp documentation for details
   *
   * @see https://doc.openerp.com/trunk/server/api_models/#openerp.osv.orm.BaseModel.write
   * @param model the model to update
   * @param ids the ids of the records to update
   * @param fields the field names and associated values to update
   * @return a Future[True]
   * @throws OpenERPException if the write fails
   */
  def write[T:TransportDataConverter](model: String, ids: List[Int], fields: Map[String, T]) : Future[Boolean] = {
    val processedFields: Future[Map[String, TransportDataType]] = validateParams(model,fields.mapValues(implicitly[TransportDataConverter[T]].write))

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()

    val result = for {
      f <- processedFields
      r <- transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "write", ids.toTransportDataType,f.toTransportDataType, context.toTransportDataType)

    } yield r

    result.onComplete({
      case Success(s) => s.fold(
        (error: String) => { logger.error(error); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result  match{
          case TransportBoolean(b) => promise.complete(Try(b))
          case fail => promise.failure(unexpected(result.toString))
        })
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  /**
   * Check the values of the fields are of the correct type
   * @param model the model that we're querying
   * @param fields the field names and values
   * @return the list of fieldName -> fieldValue tuples, formatted appropriately
   * @throws OpenERPException if the field values are invalid for the type of field
   */
  private[OpenERPSession] def validateParams(model: String, fields: Map[String, TransportDataType]): Future[Map[String, TransportDataType]] = {


      Future.sequence(

      fields.map{ case (fieldName:String, fieldValue:TransportDataType) => getFieldType(model,fieldName).map(_.map({
              //we're simplifying the write method for many2many and simply providing the 'replace' option
              case "many2many" => {
                /*  valid args are an array of ints */
                fieldValue match{
                  case x:TransportArray
                    if x.value.forall({
                      case TransportNumber(_: Int) => true
                      case _ => false
                    })
                      => fieldName -> TransportArray(List(TransportArray(List(6,0,x))))
                  case _ => throw unexpected("Invalid arguments to write to a many2many field. Expected a lit of Ints, received: " + fieldValue)
                }
              }
              //case "one2many" => // not sure about the best way to do this as there isn't an equivelent to the many2many option
              case "many2one" => {
                /* * valid args are an int or false */
                fieldValue match{
                  case TransportNumber(x:Int) => fieldName -> TransportNumber(x)
                  case x:TransportBoolean if !x.value => fieldName -> x
                  case _ => throw unexpected("Invalid arguments to write to a many2one field. Expected an Int or false, recieved: " + fieldValue)
                }

              }
              case _ => fieldName -> fieldValue
            }
          ).getOrElse(throw new OpenERPException("Field " + fieldName +  " doesn't exist?"))
      )
      }
    ).map(_.toMap)

  }

  /**
   * Delete records from model with given ids
   * @param model the model to delete from
   * @param ids the ids of the records to delete
   * @return Future[true]
   * @throws OpenERPException if there was an unexpected response from the server
   */
  def unlink(model:String, ids:List[Int]) : Future[Boolean] = {
    config.path = RPCService.RPC_OBJECT

    val promise = Promise[Boolean]()
    val result = transportAdaptor.sendRequest(config, "execute", database, uid, password, model, "unlink", ids.toTransportDataType, context.toTransportDataType)

    result.onComplete((value: Try[TransportResponse]) => value match {
      case Success(s) => s.fold((error: String) => {logger.error(error); promise.failure(new OpenERPException(error))},
        (result: TransportDataType) => result  match {
            case TransportBoolean(b) => promise.complete(Try(b))
            case fail => promise.failure(unexpected(result.toString))
          })
      case Failure(f) => promise.failure(f)
    })

    promise.future
  }

  /**
   * Call an arbitrary object method on an object
   *
   * Here we require the parameters to be specified as
   * [[com.tactix4.t4openerp.connector.transport.TransportDataType]]s
   * the reason being is as follows:
   *
   * Since we need to use varargs for the parameters it makes it problematic using context bounds
   * to specify a TransportDataConverter, as more than one parameter would quickly lead to a required
   * TransportDataConverter[Seq[Any]\]
   *
   * Hence the decision to require TransportDataTypes to be supplied. The intention being, that if a
   * 3rd party API is to be used with this method, a wrapper should be made around the API method,
   * with explicit parameter types which can internally be transformed to TransportDataTypes
   *
   * @param model the model where the method exists
   * @param methodName the method to call
   * @param params the parameters the method requires
   */

  def callMethod(model: String, methodName: String, params: TransportDataType*)  : Future[TransportDataType] = {

    config.path = RPCService.RPC_OBJECT

    val promise = Promise[TransportDataType]()
    val result = transportAdaptor.sendRequest(config, "execute", TransportString(database) :: TransportNumber(uid) :: TransportString(password) :: TransportString(model) :: TransportString(methodName) :: params.toList ++ (context.toTransportDataType :: Nil))

    result.onComplete {
      case Success(s) => s.fold((error: String) => {
        logger.error(error); promise.failure(new OpenERPException(error))
      },
        r => promise.success(r))
      case Failure(f) => promise.failure(f)
    }

    promise.future

    }

}

/**
 * Companion object to OpenERPSession providing some useful implicit conversions
 */
object OpenERPSession{
  implicit def IntToListOfInts(i: Int) : List[Int] = List(i)
  implicit def StringToListOfStrings(s: String) : List[String] = List(s)
  implicit def TupleToListOfTuples (t : (String, TransportDataType)) : List[(String, TransportDataType)] = List(t)
}
