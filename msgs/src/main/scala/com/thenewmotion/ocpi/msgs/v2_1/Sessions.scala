package com.thenewmotion.ocpi.msgs.v2_1

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.msgs.ResourceType.{Full, Patch}
import com.thenewmotion.ocpi.{Enumerable, Nameable}
import com.thenewmotion.ocpi.msgs.{CurrencyCode, Resource, ResourceType}
import Cdrs.{AuthMethod, ChargingPeriod}
import Locations.Location
import Tokens.AuthId

object Sessions {

  trait SessionId extends Any { def value: String }
  object SessionId {
    private case class SessionIdImpl(value: String) extends AnyVal with SessionId {
      override def toString: String = value
    }

    def apply(value: String): SessionId = {
      require(value.length <= 36, "Session Id must be 36 characters or less")
      SessionIdImpl(value)
    }

    def unapply(id: SessionId): Option[String] = Some(id.value)
  }

  sealed trait SessionStatus extends Nameable
  object SessionStatus extends Enumerable[SessionStatus] {
    case object Active extends SessionStatus { val name = "ACTIVE" }
    case object Completed extends SessionStatus { val name = "COMPLETED" }
    case object Invalid extends SessionStatus { val name = "INVALID" }
    case object Pending extends SessionStatus { val name = "PENDING" }
    val values = Iterable(Active, Completed, Invalid, Pending)
  }

  trait BaseSession[RT <: ResourceType] extends Resource[RT] {
    def startDatetime: RT#F[ZonedDateTime]
    def endDatetime: Option[ZonedDateTime]
    def kwh: RT#F[Int]
    def authId: RT#F[AuthId]
    def authMethod: RT#F[AuthMethod]
    def location: RT#F[Location]
    def meterId: Option[String]
    def currency: RT#F[CurrencyCode]
    def chargingPeriods: RT#F[Seq[ChargingPeriod]]
    def totalCost: Option[BigDecimal]
    def status: RT#F[SessionStatus]
    def lastUpdated: RT#F[ZonedDateTime]
  }

  case class Session(
    id: SessionId,
    startDatetime: ZonedDateTime,
    endDatetime: Option[ZonedDateTime],
    kwh: Int,
    authId: AuthId,
    authMethod: AuthMethod,
    location: Location,
    meterId: Option[String],
    currency: CurrencyCode,
    chargingPeriods: Seq[ChargingPeriod] = Nil,
    totalCost: Option[BigDecimal],
    status: SessionStatus,
    lastUpdated: ZonedDateTime
  ) extends BaseSession[Full] {
    require(location.evses.toSeq.length == 1, "Session Location must have one Evse")
    require(location.evses.flatMap(_.connectors).toSeq.length == 1, "Session Location must have one Connector")
  }

  case class SessionPatch(
    startDatetime: Option[ZonedDateTime] = None,
    endDatetime: Option[ZonedDateTime] = None,
    kwh: Option[Int] = None,
    authId: Option[AuthId] = None,
    authMethod: Option[AuthMethod] = None,
    location: Option[Location] = None,
    meterId: Option[String] = None,
    currency: Option[CurrencyCode] = None,
    chargingPeriods: Option[Seq[ChargingPeriod]] = None,
    totalCost: Option[BigDecimal] = None,
    status: Option[SessionStatus] = None,
    lastUpdated: Option[ZonedDateTime] = None
  ) extends BaseSession[Patch] {
    require(location.fold(1)(_.evses.toSeq.length) == 1, "Session Location must have one Evse")
    require(
      location.fold(1)(_.evses.flatMap(_.connectors).toSeq.length) == 1,
      "Session Location must have one Connector"
    )
  }

}
