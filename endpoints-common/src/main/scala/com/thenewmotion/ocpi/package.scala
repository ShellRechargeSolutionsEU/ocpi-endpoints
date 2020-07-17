package com.thenewmotion

import akka.http.scaladsl.server.Route
import cats.Functor
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber
import org.slf4j.{Logger, LoggerFactory}

package object ocpi {
  def Logger(cls: Class[_]): Logger = LoggerFactory.getLogger(cls)

  implicit class RichFEither[F[_]: Functor, L, R](value: F[Either[L, R]]) {
    import cats.syntax.functor._
    def mapRight[T](f: R => T): F[Either[L, T]] =
      value.map(_.map(f))
  }

  type Version = VersionNumber

  type GuardedRoute = (Version, GlobalPartyId) => Route
}
