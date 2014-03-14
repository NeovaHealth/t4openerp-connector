package com.tactix4.t4openerp.connector.transport

import com.tactix4.t4openerp.connector._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz._
import scala.language.implicitConversions

case class FutureResponse[E,A](value:Future[E \/ A]){

  def isError:Future[Boolean] = value.map(_.isLeft)

  def isResult:Future[Boolean] = value.map(_.isRight)

  def getOrElse[AA >: A](v: AA) : Future[AA] = value.map(_.getOrElse(v))

  def fold[X](f: E => X)(g: A => X): Future[X] =
    value.map(_.fold(f,g))

  def map[B](f: A => B):FutureResponse[E,B] =
    FutureResponse(value.map(_.fold(e => -\/(e),a => \/-(f(a)))))

  def failMap[F](g: E => F):FutureResponse[F,A] =
    FutureResponse(value.map(_.fold(e => -\/(g(e)),a => \/-(a))))

  def biMap[F,B](g: E => F,f: A => B):FutureResponse[F,B] =
    FutureResponse(value.map(_.fold(e => -\/(g(e)),a => \/-(f(a)))))


  def flatMap[B](f: A => FutureResponse[E,B]) : FutureResponse[E,B] =
    FutureResponse(value.flatMap(_.fold(m => Future.successful(-\/(m)),a => f(a).value)))

  def ffMap[B](f: A => E \/ B) : FutureResponse[E,B] =
    FutureResponse(value.flatMap(_.fold(m => Future.successful(-\/(m)),a => Future.successful{f(a)})))

  def ap[B](f: => FutureResponse[E,A=>B]) : FutureResponse[E,B] = {
    flatMap(a => f.map(func => func(a)))
  }
}

object FutureResponse{

  def unit[E,A](a: => A) : FutureResponse[E,A] = FutureResponse(Future.successful(\/-(a)))

  implicit def FutureOERPResultToClass[A](f: Future[ErrorMessage \/ A]) = FutureResponse(f)

  implicit def FutureResponseApplicative[L]: Applicative[({type l[a] = FutureResponse[L, a]})#l] = new Applicative[({type l[a] = FutureResponse[L, a]})#l] {
    def point[A](a: => A) = unit(a)
    def ap[A, B](fa: => FutureResponse[L, A])(f: => FutureResponse[L, A => B]) = fa ap f
  }

  implicit def FutureResponseBind[E] = new Bind[({type l[a] = FutureResponse[E, a]})#l] {
    def bind[A, B](fa: FutureResponse[E,A])(f: (A) => FutureResponse[E,B]):FutureResponse[E,B] = fa.flatMap(f)
    def map[A, B](fa:FutureResponse[E,A])(f: (A) => B):FutureResponse[E,B] = fa.map(f)
  }

  implicit def FutureResponseMonad[E] = new Monad[({type l[a] = FutureResponse[E, a]})#l]{
    def bind[A, B](fa: FutureResponse[E,A])(f: (A) => FutureResponse[E,B]): FutureResponse[E,B] = fa.flatMap(f)
    def point[A](a: => A): FutureResponse[E,A] = unit(a)
  }

}
