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
  implicit def listDecoder[T:OEDataDecoder] = OEDataDecoder[List[T]]((t:OEType) => t.array.map(a => a.map(_.decodeAs[T]).sequence[DecodeResult,T]) | DecodeResult.fail[List[T]](s"Not a list of ints: $t"))
  implicit def mapDecoder[T:OEDataDecoder] : OEDataDecoder[Map[String,T]] = OEDataDecoder[Map[String,T]]((t:OEType) => t.dict.map(
    _.mapValues(_.decodeAs[T]).foldRight(DecodeResult.ok(Map[String,T]()))((tuple, result) => {
      for {
        a <- tuple._2
        b <- DecodeResult.ok[Map[String,T]](Map(tuple._1 -> a))
        c <- result
      } yield b ++ c
  })) | DecodeResult.fail[Map[String,T]](s"Not a list of ints: $t"))

  implicit val OptionBoolDecoder = OEDataDecoder[Option[Boolean]]((t: OEType) => DecodeResult(t.bool.right[ErrorMessage]))
  implicit val OptionIntDecoder = OEDataDecoder[Option[Int]]((t: OEType) => DecodeResult(t.int.right[ErrorMessage]))
  implicit val OptionDoubleDecoder = OEDataDecoder[Option[Double]]((t: OEType) => DecodeResult(t.double.right[ErrorMessage]))
  implicit val OptionStringDecoder = OEDataDecoder[Option[String]]((t: OEType) => DecodeResult(t.string.right[ErrorMessage]))

  implicit val stringEncoder = OEDataEncoder[String]((s: String) => OEString(s))
  implicit val intEncoder = OEDataEncoder[Int]((i: Int) => OENumber(i))
  implicit val doubleEncoder = OEDataEncoder[Double]((d: Double) => OENumber(d))
  implicit val boolEncoder = OEDataEncoder[Boolean]((b: Boolean) => OEBoolean(b))
  implicit def listEncoder[T:OEDataEncoder] = OEDataEncoder[List[T]]((t:List[T]) => OEArray(t.map(_.encode)))
  implicit def mapEncoder[T:OEDataEncoder] = OEDataEncoder[Map[String,T]]((m:Map[String,T]) => OEMap(m.mapValues(_.encode)))
  implicit val optionBoolEncoder = OEDataEncoder[Option[Boolean]]((o: Option[Boolean]) => o.fold[OEType](OENull)(OEBoolean))
  implicit val optionIntEncoder = OEDataEncoder[Option[Int]]((o: Option[Int]) => o.fold[OEType](OENull)(OENumber(_)))
  implicit val optionDoubleEncoder = OEDataEncoder[Option[Double]]((o: Option[Double]) => o.fold[OEType](OENull)(OENumber(_)))
  implicit val optionStringEncoder = OEDataEncoder[Option[String]]((o: Option[String]) => o.fold[OEType](OENull)(OEString))

}
