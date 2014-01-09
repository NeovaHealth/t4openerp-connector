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
import scala.util.control.Exception._
import scala.reflect.ClassTag

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
  def toFloat:Float = implicitly[Numeric[N]].toFloat(value)
}
case class TransportBoolean(value: Boolean) extends TransportDataType{
  type T = Boolean
}
case class TransportString(value: String) extends TransportDataType{
  type T = String
}
case class TransportArray(value: List[TransportDataType]) extends TransportDataType{
  type T = List[TransportDataType]
  def ++(o:TransportArray) = TransportArray(value ++ o.value)
}
case class TransportMap(value: Map[String, TransportDataType]) extends TransportDataType{
  type T = Map[String, TransportDataType]
  def mapValues[C<:TransportDataType](f: TransportDataType => C) : TransportMap = TransportMap(value.mapValues(f))
  def filter(p: ((String,TransportDataType)) => Boolean): TransportMap = TransportMap(value.filter(p))
  def ++(o:TransportMap) = TransportMap(value ++ o.value)
  def get[N <: TransportDataType](key:String)(implicit tag:ClassTag[N]):Option[N] ={
    value.get(key).map {
      case z@tag(x) => Some(z.asInstanceOf[N])
      case _ => None
    }.flatten
  }
}
case object TransportNull extends TransportDataType{
  type T = Null
  val value: TransportNull.T = null
}







