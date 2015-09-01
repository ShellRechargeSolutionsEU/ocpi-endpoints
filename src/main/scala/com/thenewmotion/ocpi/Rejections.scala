package com.thenewmotion.ocpi

import spray.routing.Rejection

case class UnknownVersionRejection(version: String) extends Rejection
case class NoVersionsRejection() extends Rejection

