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
import com.tactix4.t4openerp.connector.codecs.OEDataEncoder

/**
 * Class to hold the context data used by OpenERP to adjust times and languages as well as to hold other
 * arbitrary data in a map
 *
 * @author max@tactix4.com
 * 5/20/13
 */

case class OEContext(activeTest: Boolean = true, lang: String = "en_GB", timezone: String = "Europe/London")


/**
 * Companion object providing an implicit instance of a TransportDataConverter[OpenERPContext]
 */
object OEContext{

  implicit def OEContextToOEType(c: OEContext):OEType = OEContextConverter.encode(c)

  /**
   * Implicit object to convert a context to a TransportDataType
   */
  implicit object OEContextConverter extends OEDataEncoder[OEContext]{
    /**
     * convert to TransportDataType
     * @param obj the context to convert
     * @return the TransportDataType representation of a context
     */
    def encode(obj: OEContext): OEType =
      OEMap("activeTest" -> obj.activeTest, "lang" -> obj.lang, "timezone" -> obj.timezone)
}}
