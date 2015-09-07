package com.thenewmotion.ocpi

import spray.routing.Rejection

case class UnsupportedVersionRejection(version: String) extends Rejection
case class NoVersionsRejection() extends Rejection

