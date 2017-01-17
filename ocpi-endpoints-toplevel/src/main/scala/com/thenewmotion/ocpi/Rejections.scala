package com.thenewmotion.ocpi

import akka.http.scaladsl.server.Rejection

case class UnsupportedVersionRejection(version: String) extends Rejection
case class NoVersionsRejection() extends Rejection

