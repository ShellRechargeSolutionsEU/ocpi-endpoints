package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshalling.{GenericMarshallers, ToRequestMarshaller, ToResponseMarshaller}
import cats.effect.{ContextShift, Effect}
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

  implicit def futureMarshaller: HktMarshallable[Future] = new HktMarshallable[Future] {
    def responseMarshaller[A: ToResponseMarshaller]: ToResponseMarshaller[Future[A]] = implicitly
    def requestMarshaller[A : ToRequestMarshaller]: ToRequestMarshaller[Future[A]] = implicitly
  }

  implicit def effectMarshaller[F[_]: Effect](implicit s: ContextShift[F], eff: Effect[F]): HktMarshallable[F] = new HktMarshallable[F] {
    def responseMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[F[A]] =
      GenericMarshallers.futureMarshaller(m).compose(io => eff.toIO(eff.flatMap(s.shift)(_ => io)).unsafeToFuture())

    def requestMarshaller[A](implicit m: ToRequestMarshaller[A]): ToRequestMarshaller[F[A]] =
      GenericMarshallers.futureMarshaller(m).compose(io => eff.toIO(eff.flatMap(s.shift)(_ => io)).unsafeToFuture())
  }
}


object HktMarshallableSyntax {
  implicit def respMarshaller[F[_], A : ToResponseMarshaller](implicit M: HktMarshallable[F]): ToResponseMarshaller[F[A]] =
    M.responseMarshaller

  implicit def reqMarshaller[F[_], A : ToRequestMarshaller](implicit M: HktMarshallable[F]): ToRequestMarshaller[F[A]] =
    M.requestMarshaller
}