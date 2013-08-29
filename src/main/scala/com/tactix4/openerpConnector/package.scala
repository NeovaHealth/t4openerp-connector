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

package com.tactix4

import scala.language.implicitConversions
import com.tactix4.openerpConnector.transport._

/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object openerpConnector{


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
  implicit def ListOfIntsToTransportArray(l: List[Int]) : TransportArray = TransportArrayType(l.map(x => TransportNumber(x)))
  implicit def ListOfStringsToTransportArray[T <: TransportDataType](l: List[String]) : TransportArray = TransportArrayType(l.map(TransportString))
}
