package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshalling.{GenericMarshallers, ToRequestMarshaller, ToResponseMarshaller}
import cats.Id
import cats.effect.{ContextShift, IO}
import scala.concurrent.ExecutionContext

/**
  * Convenience marshallers to help keep test code clean
  */
object HktMarshallableFromECInstances {

  implicit def ioCsFromEcMarshaller(implicit ec: ExecutionContext): HktMarshallable[IO] = new HktMarshallable[IO] {
    def responseMarshaller[A](implicit m: ToResponseMarshaller[A]): ToResponseMarshaller[IO[A]] = {
      implicit val s: ContextShift[IO] = IO.contextShift(ec)
      GenericMarshallers.futureMarshaller(m).compose(io => (s.shift *> io).unsafeToFuture())
    }

    def requestMarshaller[A](implicit m: ToRequestMarshaller[A]): ToRequestMarshaller[IO[A]] = {
      implicit val s: ContextShift[IO] = IO.contextShift(ec)
      GenericMarshallers.futureMarshaller(m).compose(io => (s.shift *> io).unsafeToFuture())
    }
  }

  implicit def idMarshaller: HktMarshallable[Id] = new HktMarshallable[Id] {
    def responseMarshaller[A: ToResponseMarshaller]: ToResponseMarshaller[A] = implicitly
    def requestMarshaller[A : ToRequestMarshaller]: ToRequestMarshaller[A] = implicitly
  }
}

