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

case class SuccessResp[D](
  statusCode: SuccessCode,
  statusMessage: Option[String] = None,
  timestamp: ZonedDateTime = ZonedDateTime.now,
  data: D
) extends OcpiResponse[SuccessCode]

object SuccessResp {
  def apply(
   statusCode: SuccessCode,
   statusMessage: Option[String],
   timestamp: ZonedDateTime
  ): SuccessResp[Unit] =
    SuccessResp[Unit](statusCode, statusMessage, timestamp, ())

  def apply(
   statusCode: SuccessCode
 ): SuccessResp[Unit] =
    SuccessResp(statusCode, None, ZonedDateTime.now)

  def apply(
    statusCode: SuccessCode,
    statusMessage: String
  ): SuccessResp[Unit] =
    SuccessResp(statusCode, Some(statusMessage), ZonedDateTime.now)
}