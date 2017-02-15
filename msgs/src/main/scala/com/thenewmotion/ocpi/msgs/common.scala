package com.thenewmotion.ocpi.msgs

import com.github.nscala_time.time.Imports.DateTime
import OcpiStatusCode._

sealed trait AuthToken {
  def value: String
  override def toString = value
  require(value.length <= 64)
}
case class OurAuthToken(value: String) extends AuthToken
case class TheirAuthToken(value: String) extends AuthToken

trait PartyId extends Any {
  def value: String
  override def toString = value
}

private case class PartyIdImpl(value: String) extends AnyVal with PartyId

object PartyId {
  def apply(value: String): PartyId = {
    require(value.length == 3, "PartyId must have a length of 3")
    PartyIdImpl(value)
  }
}

trait CountryCode extends Any {
  def value: String
  override def toString = value
}

private case class CountryCodeImpl(value: String) extends AnyVal with CountryCode

object CountryCode {
  def apply(value: String): CountryCode = {
    require(value.length == 2, "CountryCode must have a length of 2")
    CountryCodeImpl(value)
  }
}

case class GlobalPartyId(
  countryCode: CountryCode,
  partyId: PartyId
)

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