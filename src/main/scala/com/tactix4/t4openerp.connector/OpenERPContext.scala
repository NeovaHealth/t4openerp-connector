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

import scala.collection.mutable
import com.tactix4.t4openerp.connector.transport._
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Class to hold the context data used by OpenERP to adjust times and languages as well as to hold other
 * arbitrary data in a map
 *
 * @author max@tactix4.com
 * 5/20/13
 */

class OpenERPContext(activeTest: Boolean = true,
              lang: String,
              timezone: String) extends mutable.HashMap[String, Either[String,Boolean]] with Logging {

  val ActiveTestTag = "active_test"
  val TimezoneTag = "tz"
  val LangTag = "lang"

  setActiveTest(activeTest)
  setTimeZone(timezone)
  setLanguage(lang)

  def getActiveTest = get(ActiveTestTag)
  def getTimeZone = get(TimezoneTag)
  def getLanguage = get(LangTag)

  def setActiveTest(b: Boolean) ={logger.debug("setting ActiveTest to: " + b); put(ActiveTestTag,Right(b))}
  def setTimeZone(tz: String) = {logger.debug("setting timezone to: " + tz); put(TimezoneTag, Left(tz))}
  def setLanguage(l: String) = {logger.debug("setting language to: " + l); put(LangTag, Left(l))}

}

/**
 * Companion object providing an implicit instance of a TransportDataConverter[OpenERPContext]
 */
object OpenERPContext{

  /**
   * Implicit object to convert a context to a TransportDataType
   */
  implicit object OpenERPContextConverter$ extends TransportDataConverter[OpenERPContext]{
    /**
     * convert to TransportDataType
     * @param obj the context to convert
     * @return the TransportDataType representation of a context
     */
    def write(obj: OpenERPContext): TransportDataType =
      TransportMap(obj.toList.map(t => t._1 -> t._2.fold(TransportString, TransportBoolean)))

    /**
     * not implemented
     * @param obj
     * @return
     */
    def read(obj: TransportDataType): OpenERPContext = ???

}}
