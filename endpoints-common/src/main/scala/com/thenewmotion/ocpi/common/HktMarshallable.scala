package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshalling.{GenericMarshallers, ToRequestMarshaller, ToResponseMarshaller}
import cats.effect.{ContextShift, IO}
import scala.concurrent.Future

/**
  * Turns a higher-kinded type into a Marshaller if
  * there is a Marshaller for the type argument in implicit scope.
  *
  * The bridge between IO and akka-http.
  */
trait HktMarshallable[F[_]] {
  def responseMarshaller[A : ToResponseMarshaller]: ToResponseMarshaller[F[A]]
  def requestMarshaller[A : ToRequestMarshaller]: ToRequestMarshaller[F[A]]
}

object HktMarshallableInstances {

  import HktMarshallableSyntax._

  implicit def futureMarshaller: HktMarshallable[Future] = new HktMarshallable[scala.concurrent.Future] {
    def responseMarshaller[A: ToResponseMarshaller]: ToResponseMarshaller[Future[A]] = implicitly
    def requestMarshaller[A : ToRequestMarshaller]: ToRequestMarshaller[Future[A]] = implicitly
  }

  implicit def ioMarshaller(implicit s: ContextShift[IO]): HktMarshallable[IO] = new HktMarshallable[IO] {
    def responseMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[IO[A]] =
      GenericMarshallers.futureMarshaller(m).compose(io => (s.shift *> io).unsafeToFuture())

    def requestMarshaller[A](implicit m: ToRequestMarshaller[A]): ToRequestMarshaller[IO[A]] =
      GenericMarshallers.futureMarshaller(m).compose(io => (s.shift *> io).unsafeToFuture())
  }
}


object HktMarshallableSyntax {
  implicit def respMarshaller[F[_], A : ToResponseMarshaller](implicit M: HktMarshallable[F]): ToResponseMarshaller[F[A]] =
    M.responseMarshaller

  implicit def reqMarshaller[F[_], A : ToRequestMarshaller](implicit M: HktMarshallable[F]): ToRequestMarshaller[F[A]] =
    M.requestMarshaller
}