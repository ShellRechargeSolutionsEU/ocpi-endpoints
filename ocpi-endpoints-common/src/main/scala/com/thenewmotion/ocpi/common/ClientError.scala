package com.thenewmotion.ocpi.common

sealed trait ClientError {def reason: Option[String]}

object ClientError{

  case class NotFound(reason: Option[String] = None) extends ClientError
  case class ConnectionFailed(reason: Option[String] = None) extends ClientError
  case class UnmarshallingFailed(reason: Option[String] = None) extends ClientError
  case class UnsuccessfulResponse(reason: Option[String] = None) extends ClientError
  case class OcpiClientError(reason: Option[String] = None) extends ClientError
  case class MissingParameters(reason: Option[String] = None) extends ClientError
  case class OcpiServerError(reason: Option[String] = None) extends ClientError
  case class Unknown(reason: Option[String] = None) extends ClientError
}