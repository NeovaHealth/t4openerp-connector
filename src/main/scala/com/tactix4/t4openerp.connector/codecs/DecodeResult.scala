package com.tactix4.t4openerp.connector.codecs

import com.tactix4.t4openerp.connector._
import scalaz.\/
import scalaz._
import Scalaz._

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 25/02/14
 * Time: 18:59
 * To change this template use File | Settings | File Templates.
 */
case class DecodeResult[A](result: ErrorMessage\/A) {

 def fold[X](
    failure: ErrorMessage => X,
    value: A => X
  ): X = result.fold(m => failure(m) , value)

  def getOrElse(e: A) : A = result.getOrElse(e)

  def map[B](f: A => B) : DecodeResult[B] = DecodeResult(result map f)

  def flatMap[B](f: A => DecodeResult[B]) : DecodeResult[B] = result.fold(m => DecodeResult(m.left[B]),f)

  def toOption: Option[A] = result.toOption

  def toEither: Either[ErrorMessage,A] = result.toEither

}

object DecodeResult extends DecodeResults {

  implicit val decodeResultApplicative = new Applicative[DecodeResult] {
    def point[A](a: => A): DecodeResult[A] = DecodeResult(a.right)

    def ap[A, B](fa: => DecodeResult[A])(f: => DecodeResult[(A) => B]): DecodeResult[B] = for {
      a <- fa
      a2b <- f
    } yield a2b(a)

  }


  def ok[A](value: A): DecodeResult[A] =
    DecodeResult(value.right[ErrorMessage])

  def fail[A](m: ErrorMessage): DecodeResult[A] =
    DecodeResult(m.left[A])
}
trait DecodeResults{
  def okResult[A](value: A): DecodeResult[A] =
    DecodeResult.ok(value)

  def failResult[A](m: ErrorMessage): DecodeResult[A] =
    DecodeResult.fail(m)

}