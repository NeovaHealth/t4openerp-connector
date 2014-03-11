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
import scalaz._
import Scalaz._
import com.tactix4.t4openerp.connector.codecs._
import com.tactix4.t4openerp.connector.transport.OENumber
import com.tactix4.t4openerp.connector.transport.OEBoolean
import com.tactix4.t4openerp.connector.transport.OEString

/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object connector{
  implicit def pimpEncoder[T:OEDataEncoder](any: T) = new EncodeOps[T](any)
  implicit def pimpDecoder(any: OEType) = new DecodeOps(any)

  type ErrorMessage = String
  type Id = Int

  type OEResponse[A] = FutureResponse[ErrorMessage,A]

  implicit def stringsToOERPArray(ss:List[String]) = OEArray(ss.map(OEString.apply))
  implicit def idsToOERPArray(is:List[Id]) = OEArray(is.map(i => OENumber(i)))
  implicit def stringToOERPString(s:String) = OEString(s)
  implicit def booleanToOERPBoolean(b:Boolean)= OEBoolean(b)
  implicit def intToOERPNumber(i:Int)= OENumber(i)
  implicit def doubleToOERPNumber(d:Double)= OENumber(d)

  implicit val stringDecoder = OEDataDecoder[String]((t: OEType) => DecodeResult(t.string.map(_.right[ErrorMessage]) | (s"Not String: $t").left[String]))
  implicit val intDecoder = OEDataDecoder[Int]((t: OEType) => DecodeResult(t.int.map(_.right[ErrorMessage]) | (s"Not Int: $t").left[Int]))
  implicit val doubleDecoder = OEDataDecoder[Double]((t: OEType) => DecodeResult(t.double.map(_.right[ErrorMessage]) | (s"Not Double: $t").left[Double]))
  implicit val boolDecoder = OEDataDecoder[Boolean]((t: OEType) => DecodeResult(t.bool.map(_.right[ErrorMessage]) | (s"Not Boolean: $t").left[Boolean]))


  implicit val stringEncoder = OEDataEncoder[String]((s: String) => OEString(s))
  implicit val intEncoder = OEDataEncoder[Int]((i: Int) => OENumber(i))
  implicit val doubleEncoder = OEDataEncoder[Double]((d: Double) => OENumber(d))
  implicit val boolEncoder = OEDataEncoder[Boolean]((b: Boolean) => OEBoolean(b))

}
