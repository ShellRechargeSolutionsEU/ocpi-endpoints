package com.thenewmotion

import akka.http.scaladsl.server.Route
import ocpi.msgs.GlobalPartyId
import ocpi.msgs.Versions.VersionNumber
import org.slf4j.LoggerFactory
import cats.syntax.either._  // Keep Scala 2.11 happy

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)

  implicit class PimpedFutureEither[L, R](value: Future[Either[L, R]]) {
    def mapRight[T](f: R => T)(implicit executionContext: ExecutionContext) =
      value.map(_.map(f))
  }

  type Version = VersionNumber

  type GuardedRoute = (Version, GlobalPartyId) => Route
}
