package com.thenewmotion.ocpi.msgs

import java.time.ZonedDateTime

import OcpiStatusCode.{ErrorCode, SuccessCode}

trait OcpiResponse[Code <: OcpiStatusCode] {
  def statusCode: Code
  def statusMessage: Option[String]
  def timestamp: ZonedDateTime
}

case class ErrorResp(
  statusCode: ErrorCode,
  statusMessage: Option[String] = None,
  timestamp: ZonedDateTime = ZonedDateTime.now
) extends OcpiResponse[ErrorCode]

trait SuccessResponse extends OcpiResponse[SuccessCode]

case class SuccessResp(
  statusCode: SuccessCode,
  statusMessage: Option[String] = None,
  timestamp: ZonedDateTime = ZonedDateTime.now
) extends SuccessResponse

case class SuccessWithDataResp[D](
  statusCode: SuccessCode,
  statusMessage: Option[String] = None,
  timestamp: ZonedDateTime = ZonedDateTime.now,
  data: D
) extends SuccessResponse
