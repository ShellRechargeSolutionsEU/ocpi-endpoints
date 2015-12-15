package com.thenewmotion.ocpi.msgs.v2_0

abstract class StatusCode(val code: Int, val default_message: String) {
  def isSuccess: Boolean
}

abstract class SuccessCode(code: Int, default_message: String)
  extends StatusCode(code, default_message)
{
  require(code >= 1000 && code <= 1999, "Code not in success range.")
  def isSuccess = true
}

abstract class ErrorCode(code: Int, default_message: String)
  extends StatusCode(code, default_message)
{
  require(code >= 2000 && code <= 3999, "Code not in error range.")
  def isSuccess = false
}

abstract class ClientErrorCode(code: Int, default_message: String)
  extends ErrorCode(code, default_message)
{require(code >= 2000 && code <= 2999, "Code not in client error range.")}

abstract class ServerErrorCode(code: Int, default_message: String)
  extends ErrorCode(code, default_message)
{require(code >= 3000 && code <= 3999, "Code not in server error range.")}

object OcpiStatusCodes {
  case object GenericSuccess extends SuccessCode(1000, "Success")

  case object GenericClientFailure extends ClientErrorCode(2000, "Client error")
  case object InvalidOrMissingParameters extends ClientErrorCode(2001, "Invalid or missing parameters")
  case object AuthenticationFailed extends ClientErrorCode(2010, "Invalid authentication token")
  case object MissingHeader extends ClientErrorCode(2011, "Header not found")

  case object GenericServerFailure extends ServerErrorCode(3000, "Server error")
  case object UnableToUseApi extends ServerErrorCode(3001, "Unable to use the client's API.")
  case object UnsupportedVersion extends ServerErrorCode(3002, "Unsupported version.")
  case object MissingExpectedEndpoints extends ServerErrorCode(3003, "Missing expected endpoints.") //TODO: TNM-2013
}