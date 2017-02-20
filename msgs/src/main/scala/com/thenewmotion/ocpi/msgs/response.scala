package com.thenewmotion.ocpi.msgs

import com.github.nscala_time.time.Imports.DateTime
import OcpiStatusCode.{ErrorCode, SuccessCode}

trait OcpiResponse[Code <: OcpiStatusCode] {
  def statusCode: Code
  def statusMessage: Option[String]
  def timestamp: DateTime
}

case class ErrorResp(
                      statusCode: ErrorCode,
                      statusMessage: Option[String] = None,
                      timestamp: DateTime = DateTime.now
                    ) extends OcpiResponse[ErrorCode]

trait SuccessResponse extends OcpiResponse[SuccessCode]

case class SuccessResp(
                        statusCode: SuccessCode,
                        statusMessage: Option[String] = None,
                        timestamp: DateTime = DateTime.now
                      ) extends SuccessResponse

case class SuccessWithDataResp[D](
                                   statusCode: SuccessCode,
                                   statusMessage: Option[String] = None,
                                   timestamp: DateTime = DateTime.now,
                                   data: D
                                 ) extends SuccessResponse
