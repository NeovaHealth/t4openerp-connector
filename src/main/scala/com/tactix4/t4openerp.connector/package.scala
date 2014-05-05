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
import com.tactix4.t4openerp.connector.domain.Domain
import scala.concurrent.Future

/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object connector{

  implicit def pimpEncoder[T:OEDataEncoder](any: T) = new EncodeOps[T](any)
  implicit def pimpDecoder(any: OEType) = new DecodeOps(any)

  implicit def futureEitherToFutureEither[A,E](fe: Future[E\/A]): EitherT[Future, E, A] = EitherT(fe)

  type FutureEither[A] = EitherT[Future,ErrorMessage,A]

  type CodecResult[A] = Validation[ErrorMessage, A]
  type ErrorMessage = String
  type Id = Int

  implicit def OptionDomainToOEType(o:Option[Domain]): OEType = o.flatMap(_.encode.toOption) | OEString("")
  implicit def ContextToOEType(c:OEContext):OEType = c.encode.toOption | OEString("")

  implicit def stringsToOERPArray(ss:List[String]) = OEArray(ss.map(OEString.apply))
  implicit def idsToOERPArray(is:List[Id]) = OEArray(is.map(i => OENumber(i)))
  implicit def stringToOERPString(s:String) = OEString(s)
  implicit def booleanToOERPBoolean(b:Boolean)= OEBoolean(b)
  implicit def intToOERPNumber(i:Int)= OENumber(i)
  implicit def doubleToOERPNumber(d:Double)= OENumber(d)

  implicit val stringDecoder = OEDataDecoder[String]((t: OEType) => t.asString(_.success[String]) | s"Not String: $t".failure[String])
  implicit val intDecoder = OEDataDecoder[Int]((t: OEType) => t.asInt(_.success[ErrorMessage]) | s"Not Int: $t".failure[Int])
  implicit val doubleDecoder = OEDataDecoder[Double]((t: OEType) => t.asDouble(_.success[ErrorMessage]) | s"Not Double: $t".failure[Double])
  implicit val boolDecoder = OEDataDecoder[Boolean]((t: OEType) => t.asBool(_.success[ErrorMessage]) | s"Not Boolean: $t".failure[Boolean])
  implicit def listDecoder[T:OEDataDecoder] = OEDataDecoder[List[T]]((t:OEType) => t.asArray(a => a.map(_.decodeAs[T]).sequence[CodecResult,T]) | s"Not a list of ints: $t".failure[List[T]])
  implicit def mapDecoder[T:OEDataDecoder] : OEDataDecoder[Map[String,T]] = OEDataDecoder[Map[String,T]]((t:OEType) =>
    t.asDictionary( _.mapValues(_.decodeAs[T]).foldRight(Map[String,T]().success[ErrorMessage])((tuple,result) =>  for {
        a <- tuple._2
        b <- Map(tuple._1 -> a).success
        c <- result
      } yield b ++ c
    )) | s"Could not decode map: $t".fail[Map[String,T]])


  implicit val OptionBoolDecoder = OEDataDecoder[Option[Boolean]](_.bool.success[ErrorMessage])
  implicit val OptionIntDecoder = OEDataDecoder[Option[Int]](_.int.success[ErrorMessage])
  implicit val OptionDoubleDecoder = OEDataDecoder[Option[Double]](_.double.success[ErrorMessage])
  implicit val OptionStringDecoder = OEDataDecoder[Option[String]](_.string.success[ErrorMessage])

  implicit val stringEncoder = OEDataEncoder[String]((s: String) => OEString(s).success)
  implicit val intEncoder = OEDataEncoder[Int]((i: Int) => OENumber(i).success)
  implicit val doubleEncoder = OEDataEncoder[Double]((d: Double) => OENumber(d).success)
  implicit val boolEncoder = OEDataEncoder[Boolean]((b: Boolean) => OEBoolean(b).success)
  implicit def listEncoder[T:OEDataEncoder] = OEDataEncoder[List[T]]((t:List[T]) => t.map(_.encode).sequence[CodecResult,OEType].map(OEArray(_)))
  implicit def mapEncoder[T:OEDataEncoder] = OEDataEncoder[Map[String,T]]((m:Map[String,T]) =>
    m.map(v => (v._1,v._2.encode)).foldRight((OEDictionary():OEType).success[ErrorMessage])((tuple, result) =>
    for {
      a <- tuple._2
      b <- OEDictionary(tuple._1 -> a).success
      c <- result
    } yield OEDictionary(b.value ++ (c.dictionary | Map()))
    )
  )


  implicit val optionBoolEncoder = OEDataEncoder[Option[Boolean]](_.fold[OEType](OENull)(OEBoolean).success)
  implicit val optionIntEncoder = OEDataEncoder[Option[Int]](_.fold[OEType](OENull)(OENumber(_)).success)
  implicit val optionDoubleEncoder = OEDataEncoder[Option[Double]](_.fold[OEType](OENull)(OENumber(_)).success)
  implicit val optionStringEncoder = OEDataEncoder[Option[String]](_.fold[OEType](OENull)(OEString).success)

}
