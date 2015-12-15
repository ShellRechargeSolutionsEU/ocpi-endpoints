package com.thenewmotion

import org.slf4j.LoggerFactory
import spray.routing._

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)

  type Version = String
  type AuthToken = String
  type URI = String

  type GuardedRoute = (Version, ApiUser) => Route

  val ourVersion: Version = "2.0"
}
