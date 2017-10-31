package com.thenewmotion.ocpi.sessions

sealed trait SessionError {def reason: Option[String]}

object SessionError {
  case class SessionNotFound(reason: Option[String] = None) extends SessionError
  case class IncorrectSessionId(reason: Option[String] = None) extends SessionError
}
