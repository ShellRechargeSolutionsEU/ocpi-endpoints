package com.thenewmotion.ocpi.msgs.v2_1

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.msgs.CurrencyCode
import com.thenewmotion.ocpi.{Enumerable, Nameable}
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import com.thenewmotion.ocpi.msgs.v2_1.Tariffs.Tariff

object Cdrs {
  sealed abstract class AuthMethod(val name: String) extends Nameable
  object AuthMethod extends Enumerable[AuthMethod] {
    case object AuthRequest extends AuthMethod("AUTH_REQUEST")
    case object Whitelist extends AuthMethod("WHITELIST")
    val values = Set(AuthRequest, Whitelist)
  }

  sealed abstract class CdrDimensionType(val name: String) extends Nameable
  object CdrDimensionType extends Enumerable[CdrDimensionType] {
    case object Energy extends CdrDimensionType("ENERGY")
    case object Flat extends CdrDimensionType("FLAT")
    case object MaxCurrent extends CdrDimensionType("MAX_CURRENT")
    case object MinCurrent extends CdrDimensionType("MIN_CURRENT")
    case object ParkingTime extends CdrDimensionType("PARKING_TIME")
    case object Time extends CdrDimensionType("TIME")
    val values = Set(Energy, Flat, MaxCurrent, MinCurrent, ParkingTime, Time)
  }

  final case class CdrDimension(
    `type`: CdrDimensionType,
    volume: BigDecimal
  )

  final case class ChargingPeriod(
    startDateTime: ZonedDateTime,
    dimensions: Iterable[CdrDimension]
  )

  final case class Cdr(
    id: String,
    startDateTime: ZonedDateTime,
    stopDateTime: ZonedDateTime,
    authId: String,
    authMethod: AuthMethod,
    location: Location,
    meterId: Option[String] = None,
    currency: CurrencyCode,
    tariffs: Option[Iterable[Tariff]] = None,
    chargingPeriods: Iterable[ChargingPeriod],
    totalCost: BigDecimal,
    totalEnergy: BigDecimal,
    totalTime: BigDecimal,
    totalParkingTime: Option[BigDecimal] = None,
    remark: Option[String] = None,
    lastUpdated: ZonedDateTime
  )
}
