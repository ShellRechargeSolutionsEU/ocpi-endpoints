package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

sealed trait CreateOrUpdateResult { def httpStatusCode: StatusCode }
object CreateOrUpdateResult {
  case object Created extends CreateOrUpdateResult {
    override def httpStatusCode: StatusCode = StatusCodes.Created
  }
  case object Updated extends CreateOrUpdateResult {
    override def httpStatusCode: StatusCode = StatusCodes.OK
  }
}
