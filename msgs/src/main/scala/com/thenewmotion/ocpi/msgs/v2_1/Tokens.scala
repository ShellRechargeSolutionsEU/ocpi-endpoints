package com.thenewmotion.ocpi
package msgs.v2_1

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.msgs.ResourceType.{Full, Patch}
import com.thenewmotion.ocpi.msgs.{Language, Resource, ResourceType}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.DisplayText
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{ConnectorId, EvseUid, LocationId}

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

  trait TokenUid extends Any { def value: String }
  object TokenUid {
    private case class TokenUidImpl(value: String) extends AnyVal with TokenUid {
      override def toString: String = value
    }

    def apply(value: String): TokenUid = {
      require(value.length <= 36, "Token Uid must be 36 characters or less")
      TokenUidImpl(value)
    }

    def unapply(tokId: TokenUid): Option[String] =
      Some(tokId.value)
  }

  trait AuthId extends Any { def value: String }
  object AuthId {
    private case class AuthIdImpl(value: String) extends AnyVal with AuthId {
      override def toString: String = value
    }

    def apply(value: String): AuthId = {
      require(value.length <= 36, "Auth Id must be 36 characters or less")
      AuthIdImpl(value)
    }

    def unapply(id: AuthId): Option[String] =
      Some(id.value)
  }

  trait BaseToken[RT <: ResourceType] extends Resource[RT] {
    def uid: RT#F[TokenUid]
    def `type`: RT#F[TokenType]
    def authId: RT#F[AuthId]
    def visualNumber: Option[String]
    def issuer: RT#F[String]
    def valid: RT#F[Boolean]
    def whitelist: RT#F[WhitelistType]
    def language: Option[Language]
    def lastUpdated: RT#F[ZonedDateTime]
  }

  case class Token(
    uid: TokenUid,
    `type`: TokenType,
    authId: AuthId,
    visualNumber: Option[String] = None,
    issuer: String,
    valid: Boolean,
    whitelist: WhitelistType,
    language: Option[Language] = None,
    lastUpdated: ZonedDateTime
  ) extends BaseToken[Full]

  case class TokenPatch(
    uid: Option[TokenUid] = None,
    `type`: Option[TokenType] = None,
    authId: Option[AuthId] = None,
    visualNumber: Option[String] = None,
    issuer: Option[String] = None,
    valid: Option[Boolean] = None,
    whitelist: Option[WhitelistType] = None,
    language: Option[Language] = None,
    lastUpdated: Option[ZonedDateTime] = None
  ) extends BaseToken[Patch]

  case class LocationReferences(
    locationId: LocationId,
    evseUids: Iterable[EvseUid] = Nil,
    connectorIds: Iterable[ConnectorId] = Nil
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
