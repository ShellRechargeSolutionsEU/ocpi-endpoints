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
  extends StatusCode(code, default_message)
{require(code >= 2000 && code <= 2999, "Code not in client error range.")}

abstract class ServerErrorCode(code: Int, default_message: String)
  extends StatusCode(code, default_message)
{require(code >= 3000 && code <= 3999, "Code not in server error range.")}

object OcpiStatusCodes {
  case object GenericSuccess extends SuccessCode(1000, "Success")

  case object GenericClientError extends ErrorCode(2000, "Client error")
  case object InvalidOrMissingParametersError extends ErrorCode(2001, "Invalid or missing parameters")
  case object AuthenticationFailedError extends ErrorCode(2010, "Invalid authentication token")
  case object MissingHeaderError extends ErrorCode(2011, "Header not found")

  case object GenericServerError extends ErrorCode(3000, "Server error")
  case object UnableToUseApiError extends ErrorCode(3001, "Unable to use the client's API.")
  case object UnsupportedVersionError extends ErrorCode(3002, "Unsupported version.")
}