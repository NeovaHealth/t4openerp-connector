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
case class CodecResult[A](result: ErrorMessage\/A) {

 def fold[X](
    failure: ErrorMessage => X,
    value: A => X
  ): X = result.fold(m => failure(m) , value)

  def `|`(e:A) : A = getOrElse(e)

  def getOrElse(e: A) : A = result.getOrElse(e)

  def map[B](f: A => B) : CodecResult[B] = CodecResult(result map f)

  def flatMap[B](f: A => CodecResult[B]) : CodecResult[B] = result.fold(m => CodecResult(m.left[B]),f)

  def toOption: Option[A] = result.toOption

  def toEither: Either[ErrorMessage,A] = result.toEither

}

object CodecResult extends CodecResults {

  implicit val decodeResultApplicative: Applicative[CodecResult]  = new Applicative[CodecResult] {
    def point[A](a: => A): CodecResult[A] = CodecResult(a.right)

    def ap[A, B](fa: => CodecResult[A])(f: => CodecResult[(A) => B]): CodecResult[B] = for {
      a <- fa
      a2b <- f
    } yield a2b(a)

  }



  def ok[A](value: A): CodecResult[A] =
    CodecResult(value.right[ErrorMessage])

  def fail[A](m: ErrorMessage): CodecResult[A] =
    CodecResult(m.left[A])
}
trait CodecResults{
  def okResult[A](value: A): CodecResult[A] =
    CodecResult.ok(value)

  def failResult[A](m: ErrorMessage): CodecResult[A] =
    CodecResult.fail(m)

}