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
import com.tactix4.t4openerp.connector.domain.Domain
import com.typesafe.scalalogging.slf4j.Logging
import scala.language.implicitConversions
import scala.concurrent.Future

import scalaz._
import Scalaz._

/**
 * An OpenERPSession represents a current session to an OpenERP server.
 *
 * It is the main class that a user will interact with - allowing access to common ORM methods
 *
 * @example {{{
 *                     val session = new OpenERPConnector("http", openerpHost,openerpPort).startSession(username,password,database)
 *
 *                     val result = for {
 *                        s <- session
 *                        id <- s.read('res.users', 1)
 *                     } yield id
 *
 *                     result.onComplete((value: Try[ResultType]) => value match {
 *                        case Success(s) => s.foreach(_ foreach println)
 *                        case Failure(f) => println(f)
 *                     })
 *
 *          }}}
 */


case class OESession(uid: OEResponse[Id], transportAdaptor: OETransportAdaptor, config: OETransportConfig, database: String, password: String, context: OEContext = OEContext(true, "en_GB", "Europe/London")) extends Logging {

  def isLoggedIn: Future[Boolean] = uid.isResult

  /**
   * Searches the openerp server with the following parameters
   *
   * @param domain an [[scala.Option]] contain a [[com.tactix4.t4openerp.connector.domain.Domain]] object - default is None
   * @param offset an [[scala.Int]] indicating the number of records to skip - default is 0
   * @param limit an [[scala.Int]] indicating the maximum number of records to return - default is 0
   * @param order a [[java.lang.String]] indicating the column name by which to sort the records - server default is "id"
   * @return a [[com.tactix4.t4openerp.connector.transport.FutureResponse]] containing a [[scala.collection.immutable.List]] of type [[scala.Int]] representing the ids of the matching records
   * @throws OpenERPException if the response from the server does not meet our expectations - or if we receive an exception from the [[OETransportAdaptor]]
   */

  def search(model: String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = ""): OEResponse[List[Id]] = {

    val result = uid.flatMap(i => transportAdaptor.sendRequest(config, "execute", database, i, password, model, "search", domain.map(_.encode) | "", offset, limit, order, context))

    result.ffMap(r => {
      val idsO = for {
        a <- r.array
        z <- a.map(_.int).sequence[Option, Int]
      } yield z.right[ErrorMessage]

      idsO | s"Unexpected response from a search request: $r".left[List[Id]]
    })

  }

  /**
   * Read/fetch the supplied ids from the openERP server
   *
   * @param ids a [[scala.collection.immutable.List]] of type [[scala.Int]] representing the ids of the records to read
   * @param fieldNames a [[scala.collection.immutable.List[ S t r i n g]]]
   * @return
   * @throws OpenERPException if the response from the server does not meet our expectations - or if we receive an exception from the [[OETransportAdaptor]]
   */

  def read(model: String, ids: List[Int], fieldNames: List[String] = Nil): OEResponse[List[OEMap]] = {

    val result = uid.flatMap(i => transportAdaptor.sendRequest(config, "execute", database, i, password, model, "read", ids, fieldNames, context))

    result.ffMap(r => {
      val rec = for {
        array <- r.array
        s <- array.flatMap(_.dict.map(OEMap.apply)).some
      } yield s.right[ErrorMessage]

      rec | s"Unexpected response from a read request: $result".left[List[OEMap]]
    })

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
  def searchAndRead(model: String, domain: Option[Domain] = None, fields: List[String] = Nil, offset: Int = 0, limit: Int = 0, order: String = ""): OEResponse[List[OEMap]] = {
    for {
      ids <- search(model, domain, offset, limit, order)
      result <- read(model, ids, fields)
    } yield result

  }


  /**
   * Create a new record
   * @param model the model within which to create the record
   * @param fields the fieldnames and values to write
   * @return a Future containing the id of the new record
   * @throws OpenERPException if the creation fails
   */
  def create(model: String, fields: Map[String, OEType]): OEResponse[Id] = {

    val result = uid.flatMap(i => transportAdaptor.sendRequest(config, "execute", database, i, password, model, "create", OEMap(fields), context))

    result.ffMap(t => t.int.map(_.right[ErrorMessage]) | s"Unexpected response from a create request: $t".left[Id])
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
  def write(model: String, ids: List[Int], fields: Map[String, OEType]): OEResponse[Boolean] = {

    val result = uid.flatMap(i => transportAdaptor.sendRequest(config, "execute", database, i, password, model, "write", ids, OEMap(fields), context))

    result.ffMap(b => b.bool.map(_.right[ErrorMessage]) | s"Unexpected response from a write request: $b".left[Boolean])


  }

  /**
   * Delete records from model with given ids
   * @param model the model to delete from
   * @param ids the ids of the records to delete
   * @return Future[true]
   */
  def unlink(model: String, ids: List[Id]): OEResponse[Boolean] = {

    val result = uid.flatMap(i => transportAdaptor.sendRequest(config, "execute", database, i, password, model, "unlink", ids, context))

    result.ffMap(b => b.bool.map(
      _.right[ErrorMessage]) | s"Unexpected response from an unlink request: $b".left[Boolean])
  }


  def callMethod(model: String, methodName: String, params: OEType*): OEResponse[OEType] = {

    uid.flatMap(i => {
      transportAdaptor.sendRequest(config, "execute", List[OEType](database, i, password, model, methodName) ++ params.toList ++ List[OEType](context))
    })

  }

}

