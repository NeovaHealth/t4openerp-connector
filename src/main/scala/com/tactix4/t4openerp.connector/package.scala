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

package com.tactix4.t4openerp

import scala.language.implicitConversions
import com.tactix4.t4openerp.connector.transport._
import scala.reflect.runtime.universe._
/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object connector{

  implicit object AnyToTransportDataType extends TransportDataConverter[Any] {
    def read(obj: TransportDataType): Any = obj.value
    def write(obj: Any): TransportDataType = obj match {
      case x: Numeric[_] => TransportNumber(x)
      case x: String  => TransportString(x)
      case x: Boolean => TransportBoolean(x)
      case x: Map[_,_]  => TransportMapType(x.toList.map(s => (s._1.toString, write(s._2))))
      case List(x:(_, _),_*) => TransportMapType(obj.asInstanceOf[List[(_,_)]].map(y => y._1.toString -> write(y._2)))
      case x: List[_] => TransportArrayType(x.map(write))
      case x => TransportString(x.toString)
    }
  }

  type TransportArray = TransportArrayType[TransportDataType]
  type TransportMap = TransportMapType[TransportDataType]
  type TransportResponse = Either[String,TransportDataType]

  type ResultType = List[List[(String, Any)]]

  implicit def pimpAny[T](any: T) = new PimpedAny(any)

  implicit def StringToTransportString(s: String) = new TransportString(s)
  implicit def BoolToTransportBool(b:Boolean) = new TransportBoolean(b)
  implicit def IntToTransportNumber(i: Int) = new TransportNumber(i)
  implicit def FloatToTransportNumber(i: Float) = new TransportNumber(i)
  implicit def DoubleToTransportNumber(i: Double) = new TransportNumber(i)
  implicit def MapOfStringsToTransportMap(m: Map[String,String]) = new TransportMap(m.toList.map((t: (String,Any)) => (t._1, t._2.toTransportDataType)))
  implicit def ListOfIntsToTransportArray(l: List[Int]) : TransportArray = TransportArrayType(l.map(x => TransportNumber(x)))
  implicit def ListOfStringsToTransportArray[T <: TransportDataType](l: List[String]) : TransportArray = TransportArrayType(l.map(TransportString))
}
