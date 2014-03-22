package com.tactix4.t4openerp.connector.transport

import com.tactix4.t4openerp.connector._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import Scalaz._
import scala.language.implicitConversions

case class FutureResult[E,A](value:Future[Validation[E,A]]){

  def isError:Future[Boolean] = value.map(_.isFailure)

  def isResult:Future[Boolean] = value.map(_.isSuccess)

  def `|`[AA>: A](v:AA): Future[AA] = getOrElse(v)

  def getOrElse[AA >: A](v: AA) : Future[AA] = value.map(_.getOrElse(v))

  def fold[X](f: E => X)(g: A => X): Future[X] =
    value.map(_.fold(f,g))

  def map[B](f: A => B):FutureResult[E,B] =
    FutureResult(value.map(_.map(f)))

  def failMap[F](g: E => F):FutureResult[F,A] =
    FutureResult(value.map(_.swapped(_.map(g))))

  def biMap[F,B](g: E => F,f: A => B):FutureResult[F,B] =
    FutureResult(value.map(_.bimap(g,f)))

  def flatMap[B](f: A => FutureResult[E,B]) : FutureResult[E,B] =
    FutureResult(value.flatMap(_.fold(e => Future.successful(e.failure),x => f(x).value)))

  def ffMap[B](f: A => Validation[E,B]) : FutureResult[E,B] =
    FutureResult(value.flatMap(_.fold(e => Future.successful(e.failure),a => Future.successful(f(a)))))

  def ap[B](f: => FutureResult[E,A=>B]) : FutureResult[E,B] = {
    flatMap(a => f.map(func => func(a)))
  }
}

object FutureResult{

  implicit def FutureOERPResultToClass[A](f: Future[Validation[ErrorMessage, A]]) = FutureResult(f)

  def unit[E,A](a: => A) : FutureResult[E,A] = FutureResult(Future.successful(a.success))


  implicit def FutureResponseApplicative[L]: Applicative[({type l[a] = FutureResult[L, a]})#l] = new Applicative[({type l[a] = FutureResult[L, a]})#l] {
    def point[A](a: => A) = unit(a)
    def ap[A, B](fa: => FutureResult[L, A])(f: => FutureResult[L, A => B]) = fa ap f
  }

  implicit def FutureResponseBind[E] = new Bind[({type l[a] = FutureResult[E, a]})#l] {
    def bind[A, B](fa: FutureResult[E,A])(f: (A) => FutureResult[E,B]):FutureResult[E,B] = fa.flatMap(f)
    def map[A, B](fa:FutureResult[E,A])(f: (A) => B):FutureResult[E,B] = fa.map(f)
  }

  implicit def FutureResponseMonad[E] = new Monad[({type l[a] = FutureResult[E, a]})#l]{
    def bind[A, B](fa: FutureResult[E,A])(f: (A) => FutureResult[E,B]): FutureResult[E,B] = fa.flatMap(f)
    def point[A](a: => A): FutureResult[E,A] = unit(a)
  }

}
