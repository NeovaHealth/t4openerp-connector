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
case class TransportNumber[N: Numeric](value: N)extends TransportDataType{
  type T = N
}
case class TransportBoolean(value: Boolean) extends TransportDataType{
  type T = Boolean
}
case class TransportString(value: String) extends TransportDataType{
  type T = String
}
case class TransportArrayType[T <: TransportDataType](input: List[T]) extends TransportDataType{
  val value = input.map(_.value)
  type T = List[_]
}
case class TransportMapType[T <: TransportDataType](input: List[(String, T)]) extends TransportDataType{
  val value = input.map(x => (x._1, x._2.value))
  type T = List[(String, _)]
}
object TransportNull extends TransportDataType{
  type T = Null
  val value: TransportNull.T = null
}







