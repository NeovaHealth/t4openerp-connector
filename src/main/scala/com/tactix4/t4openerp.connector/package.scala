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
import com.tactix4.t4openerp.connector.transport.PimpedAny
/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object connector{
  implicit def pimpAny[T](any: T) = new PimpedAny(any)


  implicit object AnyValToTransportDataType extends TransportDataConverter[AnyVal] {
    def read(obj: TransportDataType): AnyVal = ???
    def write(obj: AnyVal): TransportDataType = obj match {
      case x: Int => TransportNumber(x)
      case x: Double => TransportNumber(x)
      case x: Float => TransportNumber(x)
      case x: Long => TransportNumber(x)
      case x: Char  => TransportNumber(x)
      case x: Short => TransportNumber(x)
      case x: Boolean => TransportBoolean(x)
      case x: Unit => TransportNull
    }
  }

  implicit object StringToTDT extends TransportDataConverter[String] {
    def read(obj: TransportDataType) = obj.toString
    def write(obj: String) = TransportString(obj)

  }

  implicit object ListToTDT extends TransportDataConverter[List[_]] {
    def read(obj:TransportDataType) = ???
    def write(obj:List[_]) : TransportDataType = TransportArray(obj.map(_.toTransportDataType))
  }

  implicit object ListIntToTDT extends TransportDataConverter[List[Int]] {
    def read(obj:TransportDataType) = ???
    def write(obj:List[Int]) : TransportDataType = TransportArray(obj.map((_:AnyVal).toTransportDataType))
  }

  implicit object ListStringToTDT extends TransportDataConverter[List[String]] {
    def read(obj:TransportDataType) = ???
    def write(obj:List[String]) : TransportDataType = TransportArray(obj.map(_.toTransportDataType))

  }

  implicit object MapStringAnyToTDT extends TransportDataConverter[Map[String,Any]] {
    def read(obj:TransportDataType) = ???
    def write(obj:Map[String,Any]) : TransportDataType = TransportMap(obj.mapValues(_.toTransportDataType))
  }


  type TransportResponse = Either[String,TransportDataType]

  type ResultType = List[Map[String, Any]]


  implicit def StringToTransportString(s: String) = new TransportString(s)
  implicit def BoolToTransportBool(b:Boolean) = new TransportBoolean(b)
  implicit def IntToTransportNumber(i: Int) = new TransportNumber(i)
  implicit def FloatToTransportNumber(i: Float) = new TransportNumber(i)
  implicit def DoubleToTransportNumber(i: Double) = new TransportNumber(i)
//  implicit def MapOfStringsToTransportMap(m: Map[String,String]) = new TransportMap(m.toList.map((t: (String,Any)) => (t._1, t._2.toTransportDataType)))
//  implicit def ListOfIntsToTransportArray(l: List[Int]) : TransportArray = TransportArray(l.map(x => TransportNumber(x)))
//  implicit def ListOfStringsToTransportArray[T <: TransportDataType](l: List[String]) : TransportArray = TransportArray(l.map(TransportString))
}
