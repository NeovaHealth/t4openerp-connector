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

package com.tactix4.t4openerp.connector.transport


/**
 * @author max@tactix4.com
 *         24/08/2013
 */

/**
 * Hierarchy for the internal data representation
 *
 * This subset of datastructures/types were chosen given the overlap between
 * the supported types in json and xmlrpc
 */


sealed trait TransportDataType{
  type T
  val value : T
  override def toString: String = value.toString
}
case class TransportNumber[N:Numeric](value: N) extends TransportDataType{
  type T = N
  def toDouble:Double = implicitly[Numeric[N]].toDouble(value)
  def toInt:Int = implicitly[Numeric[N]].toInt(value)
}
case class TransportBoolean(value: Boolean) extends TransportDataType{
  type T = Boolean
}
case class TransportString(value: String) extends TransportDataType{
  type T = String
}
case class TransportArray(value: List[TransportDataType]) extends TransportDataType{
  type T = List[TransportDataType]
}
case class TransportMap(value: Map[String, TransportDataType]) extends TransportDataType{
  type T = Map[String, TransportDataType]
}
object TransportNull extends TransportDataType{
  type T = Null
  val value: TransportNull.T = null
}







