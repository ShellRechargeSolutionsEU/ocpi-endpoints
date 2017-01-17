package com.thenewmotion.ocpi
package msgs.v2_1

import org.joda.time.DateTime

object Tokens {
  sealed abstract class TokenType(val name: String) extends Nameable
  object TokenType extends Enumerable[TokenType] {
    case object Other extends TokenType("OTHER")
    case object Rfid extends TokenType("RFID")
    val values = Set(Other, Rfid)
  }

  sealed abstract class WhitelistType(val name: String) extends Nameable
  object WhitelistType extends Enumerable[WhitelistType] {
    case object Always extends WhitelistType("ALWAYS")
    case object Allowed extends WhitelistType("ALLOWED")
    case object AllowedOffline extends WhitelistType("ALLOWED_OFFLINE")
    case object Never extends WhitelistType("NEVER")
    val values = Set(Always, Allowed, AllowedOffline, Never)
  }

  case class Token(
    uid: String,
    `type`: TokenType,
    authId: String,
    visualNumber: Option[String] = None,
    issuer: String,
    valid: Boolean,
    whitelist: WhitelistType,
    language: Option[String] = None,
    lastUpdated: DateTime
  )

  case class TokenPatch(
    uid: String,
    `type`: Option[TokenType] = None,
    authId: Option[String] = None,
    visualNumber: Option[String] = None,
    issuer: Option[String] = None,
    valid: Option[Boolean] = None,
    whitelist: Option[WhitelistType] = None,
    language: Option[String] = None
  )
}
