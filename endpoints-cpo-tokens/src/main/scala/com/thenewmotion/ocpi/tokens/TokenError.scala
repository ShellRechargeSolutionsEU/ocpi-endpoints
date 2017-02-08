package com.thenewmotion.ocpi
package tokens

sealed trait TokenError {
  def reason: Option[String]
}
object TokenError {
  case class TokenNotFound(reason: Option[String] = None) extends TokenError
  case class TokenCreationFailed(reason: Option[String] = None) extends TokenError
  case class TokenUpdateFailed(reason: Option[String] = None) extends TokenError
}
