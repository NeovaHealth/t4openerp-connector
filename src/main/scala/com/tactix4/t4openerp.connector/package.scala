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

import com.tactix4.t4openerp.connector.codecs._
import com.tactix4.t4openerp.connector.domain.Domain
import com.tactix4.t4openerp.connector.transport._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.{-\/, EitherT, \/}
import scalaz.std.option.optionSyntax._
import scalaz.syntax.either._
import scalaz.syntax.traverse._
import scalaz.std.list._

/**
 * @author max@tactix4.com
 *         14/07/2013
 */
package object connector{

  implicit def executionContextToFutureMonad(implicit ec:ExecutionContext) = scalaz.std.scalaFuture.futureInstance

  implicit class EncodeOps[T:OEDataEncoder](any: T) {
    def encode: CodecResult[OEType] = implicitly[OEDataEncoder[T]].encode(any)
  }
  implicit class DecodeOps(any: OEType) {
    def decodeAs[T:OEDataDecoder] : CodecResult[T] = implicitly[OEDataDecoder[T]].decode(any)
  }

  implicit class FutureEitherP[A](a: ErrorMessage \/ A){
      def asOER : OEResult[A] = EitherT[Future,ErrorMessage,A](Future.successful(a))
  }

  implicit def futureEitherToFutureEither[A](fe: Future[ErrorMessage\/A]): OEResult[A] = EitherT(fe)

  type OEResult[A] = EitherT[Future,ErrorMessage,A]
  type CodecResult[A] = ErrorMessage \/ A
  type ErrorMessage = com.tactix4.t4xmlrpc.ErrorMessage

  implicit def OptionDomainToOEType(o:Option[Domain]): OEType = o.flatMap(_.encode.toOption) | OEString("")
  implicit def ContextToOEType(c:OEContext):OEType = c.encode.toOption | OEString("")
//
  implicit def stringsToOERPArray(ss:List[String]) = OEArray(ss.map(OEString.apply))
  implicit def idsToOERPArray(is:List[Int]) = OEArray(is.map(i => OENumber(i)))
  implicit def stringToOERPString(s:String) = OEString(s)
  implicit def booleanToOERPBoolean(b:Boolean)= OEBoolean(b)
  implicit def intToOERPNumber(i:Int)= OENumber(i)
  implicit def doubleToOERPNumber(d:Double)= OENumber(d)
//
  implicit val stringDecoder: OEDataDecoder[String] = OEDataDecoder[String]((t: OEType) => t.string \/> s"Not String: $t" )
  implicit val intDecoder = OEDataDecoder[Int]((t: OEType) => t.int \/> s"Not Int: $t")
  implicit val doubleDecoder = OEDataDecoder[Double]((t: OEType) => t.double \/> s"Not Double: $t")
  implicit val boolDecoder = OEDataDecoder[Boolean]((t: OEType) => t.bool \/> s"Not Boolean: $t")
  implicit def listDecoder[T:OEDataDecoder] = OEDataDecoder[List[T]]((t:OEType) => t.asArray(a => a.map(_.decodeAs[T]).sequence[CodecResult,T]) | -\/(s"Not a list of ints: $t"))
  implicit def mapDecoder[T:OEDataDecoder] : OEDataDecoder[Map[String,T]] = OEDataDecoder[Map[String,T]]((t:OEType) =>
    t.asDictionary( _.mapValues(_.decodeAs[T]).foldRight(Map[String,T]().right[ErrorMessage])(
      (tuple,result) =>
        for {
          a <- tuple._2
          b <- Map(tuple._1 -> a).right[ErrorMessage]
          c <- result
        } yield b ++ c
    )) | s"Could not decode map: $t".left[Map[String,T]])


  implicit val OptionBoolDecoder = OEDataDecoder[Option[Boolean]](_.bool.right[ErrorMessage])
  implicit val OptionIntDecoder = OEDataDecoder[Option[Int]](_.int.right[ErrorMessage])
  implicit val OptionDoubleDecoder = OEDataDecoder[Option[Double]](_.double.right[ErrorMessage])
  implicit val OptionStringDecoder = OEDataDecoder[Option[String]](_.string.right[ErrorMessage])
//
  implicit val stringEncoder = OEDataEncoder[String]((s: String) => OEString(s).right)
  implicit val intEncoder = OEDataEncoder[Int]((i: Int) => OENumber(i).right)
  implicit val doubleEncoder = OEDataEncoder[Double]((d: Double) => OENumber(d).right)
  implicit val boolEncoder = OEDataEncoder[Boolean]((b: Boolean) => OEBoolean(b).right)
  implicit def listEncoder[T:OEDataEncoder] = OEDataEncoder[List[T]]((t:List[T]) => t.map(_.encode).sequence[CodecResult,OEType].map(OEArray(_)))
  implicit def mapEncoder[T:OEDataEncoder] = OEDataEncoder[Map[String,T]]((m:Map[String,T]) =>
    m.map(v => (v._1,v._2.encode)).foldRight((OEDictionary():OEType).right[ErrorMessage])((tuple, result) =>
    for {
      a <- tuple._2
      b <- OEDictionary(tuple._1 -> a).right
      c <- result
    } yield OEDictionary(b.value ++ (c.dictionary | Map()))
    )
  )


  implicit val optionBoolEncoder = OEDataEncoder[Option[Boolean]](_.fold[OEType](OENull)(OEBoolean).right)
  implicit val optionIntEncoder = OEDataEncoder[Option[Int]](_.fold[OEType](OENull)(OENumber(_)).right)
  implicit val optionDoubleEncoder = OEDataEncoder[Option[Double]](_.fold[OEType](OENull)(OENumber(_)).right)
  implicit val optionStringEncoder = OEDataEncoder[Option[String]](_.fold[OEType](OENull)(OEString).right)

}
