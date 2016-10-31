package com.thenewmotion

import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber
import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber._
import org.slf4j.LoggerFactory
import spray.routing._

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)

  type Version = VersionNumber
  type AuthToken = String
  type URI = String

  type GuardedRoute = (Version, ApiUser) => Route

  val ourVersion: Version = `2.1`
}
