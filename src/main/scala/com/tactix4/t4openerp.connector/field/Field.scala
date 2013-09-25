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

package com.tactix4.t4openerp.connector.field

import com.tactix4.t4openerp.connector._
import scala.language.implicitConversions

/**
 * represents an OpenERP field
 *
 * @see https://doc.openerp.com/trunk/server/03_module_dev_02/#fields-introduction
 * @param name the name of the field
 * @param parameters a map of the fields parameters
 * @author max@tactix4.com
 */
case class Field(name: String, parameters: TransportMap){
  /**
   * get a value from the parameters list
   * @param s the 'key' of the value to get
   * @return an [[scala.Option[String]] containing the result
   */
  def get(s: String):Option[String] = {
    parameters.value.find(_._1 == s).map(_._2.toString)
  }
}