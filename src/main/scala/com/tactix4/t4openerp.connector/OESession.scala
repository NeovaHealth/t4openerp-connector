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

import com.tactix4.t4openerp.connector.domain.Domain
import com.tactix4.t4openerp.connector.transport._
import com.typesafe.scalalogging.slf4j.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.EitherT
import scalaz.std.option._
import scalaz.std.option.optionSyntax._
import scalaz.std.string._
import scalaz.std.list._
import scalaz.syntax.traverse._


case class OESession(uid: OEResult[Int], transportAdaptor: OETransportAdaptor, config: OETransportConfig, database: String, password: String, context: OEContext = OEContext(true, "en_GB", "Europe/London"))(implicit ec:ExecutionContext) extends LazyLogging {

  /**
   * Search the supplied model with the optional domain
   * @param model the model to search
   * @param domain the domain to apply to the search
   * @param offset the number of records to skip
   * @param limit a limit on the number of results
   * @param order the field name by which to order the results
   * @return an [[OEResult[List[Int]]]] of the resultant Int
   */
  def search(model: String, domain: Option[Domain] = None, offset: Int = 0, limit: Int = 0, order: String = ""): OEResult[List[Int]] = {

    for {
      r <- callMethod(model, "search", domain, offset, limit, order)
      v <- (r.array.flatMap(_.map(_.int).sequence[Option,Int]) \/> s"Unexpected response from a search request: $r").asOER
    } yield v

  }

  /**
   * Read/fetch the supplied ids from the openERP server
   *
   * @param ids a [[scala.collection.immutable.List]] of type [[scala.Int]] representing the ids of the records to read
   * @param fieldNames a [[scala.collection.immutable.List[String]]] specifying the names of the fields to return
   * @return a List of OEDictionaries, wrapped in an [[OEResult]]
   */

  def read(model: String, ids: List[Int], fieldNames: List[String] = Nil): OEResult[List[Map[String,OEType]]] =
    for {
      r <- callMethod(model, "read", ids, fieldNames)
      a <- (r.array \/> s"Response was not a an array: $r").asOER
      v <- (a.map(_.dictionary).sequence[Option, Map[String,OEType]] \/> s"Response was not an array of maps: $r").asOER
    } yield v

  /**
   * Combines the search and the read operations for convenience
   * @param model the model to query
   * @param domain the optional domain to filter the query
   * @param fields the fields to return
   * @param offset the number of records to skip
   * @param limit the maximum number of records to return
   * @param order the column name by which to sort the results
   * @return a List of OEDictionaries, wrapped in an [[OEResult]]
   */
  def searchAndRead(model: String, domain: Option[Domain] = None, fields: List[String] = Nil, offset: Int = 0, limit: Int = 0, order: String = ""): OEResult[List[Map[String,OEType]]] = {

    for {
      ids <- search(model, domain, offset, limit, order)
      if ids.nonEmpty
      result <- read(model, ids, fields)
    } yield result

  }


  /**
   * Create a new record
   * @param model the model within which to create the record
   * @param fields the fieldnames and values to write
   * @return a FutureEither containing the id of the new record
   */
  def create(model: String, fields: Map[String, OEType]): OEResult[Int] = {

    for {
      r <- callMethod(model,"create",OEDictionary(fields))
      v <- (r.int \/> s"Unexpected response from a create request: $r").asOER
    } yield v

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
   * @return a FutureEither[True]
   */
  def write(model: String, ids: List[Int], fields: Map[String, OEType]): OEResult[Boolean] = {

    for {
      r <- callMethod(model,"write", ids, OEDictionary(fields))
      v <- (r.bool \/> s"Expected a Boolean response from a write request, got: $r").asOER
    } yield v


  }

  /**
   * Delete records from model with given ids
   * @param model the model to delete from
   * @param ids the ids of the records to delete
   * @return FutureEither[true]
   */
  def unlink(model: String, ids: List[Int]): OEResult[Boolean] = {

    for {
      r <- callMethod(model,"unlink",ids)
      v <- (r.bool \/>"Unexpected response from an unlink request: $b").asOER
    } yield v

  }


  /**
   * Call an arbitrary method
   * @param model the model to call the method on
   * @param methodName the method to call
   * @param params the method parameters
   * @return an FutureEither[OEType]
   */
  def callMethod(model: String, methodName: String, params: OEType*): OEResult[OEType] = {

    for {
     i <- uid
     r <- transportAdaptor.sendRequest(config, "execute", List[OEType](database, i, password ,model, methodName) ++ params.toList ++ List[OEType](context))
    } yield r

  }

}

