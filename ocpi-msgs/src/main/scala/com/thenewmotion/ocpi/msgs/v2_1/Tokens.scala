package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.DisplayText
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
  ) {
    require(language.fold(true)(_.length == 2), "Token needs 2-letter, ISO 639-1 language code")
  }

  case class TokenPatch(
    uid: Option[String] = None,
    `type`: Option[TokenType] = None,
    authId: Option[String] = None,
    visualNumber: Option[String] = None,
    issuer: Option[String] = None,
    valid: Option[Boolean] = None,
    whitelist: Option[WhitelistType] = None,
    language: Option[String] = None,
    lastUpdated: Option[DateTime] = None
  ) {
    require(language.fold(true)(_.length == 2), "Token needs 2-letter, ISO 639-1 language code")
  }

  case class LocationReferences(
    locationId: String,
    evseUids: Iterable[String] = Nil,
    connectorIds: Iterable[String] = Nil
  )

  sealed abstract class Allowed(val name: String) extends Nameable
  object Allowed extends Enumerable[Allowed] {
    case object Allowed extends Allowed("ALLOWED")
    case object Blocked extends Allowed("BLOCKED")
    case object Expired extends Allowed("EXPIRED")
    case object NoCredit extends Allowed("NO_CREDIT")
    case object NotAllowed extends Allowed("NOT_ALLOWED")

    val values = Set(Allowed, Blocked, Expired, NoCredit, NotAllowed)
  }

  case class AuthorizationInfo(
    allowed: Allowed,
    location: Option[LocationReferences] = None,
    info: Option[DisplayText] = None
  )
}
