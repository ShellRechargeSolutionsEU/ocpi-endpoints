package com.thenewmotion.ocpi.msgs.v2_1

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.{Enumerable, Nameable}
import com.thenewmotion.ocpi.msgs.CurrencyCode
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.{AuthMethod, ChargingPeriod}
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.AuthId

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
    case object Active extends SessionStatus {val name = "ACTIVE"}
    case object Completed extends SessionStatus {val name = "COMPLETED"}
    case object Invalid extends SessionStatus {val name = "INVALID"}
    case object Pending extends SessionStatus {val name = "PENDING"}
    val values = Iterable(Active, Completed, Invalid, Pending)
  }

  case class Session(
    id: SessionId,
    startDatetime: ZonedDateTime,
    endDatetime: ZonedDateTime,
    kwh: Int,
    authId: AuthId,
    authMethod: AuthMethod,
    location: Location,
    meterId: Option[String],
    currency: CurrencyCode,
    chargingPeriods: Option[Seq[ChargingPeriod]],
    totalCost: Int,
    status: SessionStatus,
    lastUpdated: ZonedDateTime
  )

}
