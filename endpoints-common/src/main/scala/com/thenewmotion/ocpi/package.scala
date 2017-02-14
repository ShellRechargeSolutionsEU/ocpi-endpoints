package com.thenewmotion

import akka.http.scaladsl.server.Route
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.GlobalPartyId
import ocpi.msgs.v2_1.Versions.VersionNumber
import ocpi.msgs.v2_1.Versions.VersionNumber._
import org.joda.time.format.ISODateTimeFormat
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scalaz.\/

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)

  implicit class PimpedFutureDisj[L, R](value: Future[L \/ R]) {
    def mapRight[T](f: R => T)(implicit executionContext: ExecutionContext) =
      value.map(_.map(f))
  }

  type Version = VersionNumber
  type AuthToken = String
  type URI = String

  type GuardedRoute = (Version, GlobalPartyId) => Route

  val ourVersion: Version = `2.1`

  val formatterNoMillis = ISODateTimeFormat.dateTimeNoMillis.withZoneUTC
}
