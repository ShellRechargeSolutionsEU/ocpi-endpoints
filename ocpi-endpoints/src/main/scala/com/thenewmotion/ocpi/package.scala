package com.thenewmotion

import org.slf4j.LoggerFactory
import spray.routing._

package object ocpi {
  def Logger(cls: Class[_]) = LoggerFactory.getLogger(cls)

  type Version = String
  type AuthToken = String

  type GuardedRoute = (Version, AuthToken) => Route

  val version: Version = "2.0"
}
