package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.DisplayText
import com.thenewmotion.ocpi.msgs.v2_1.Locations.EnergyMix
import com.thenewmotion.ocpi.msgs.{CurrencyCode, Url}
import com.thenewmotion.ocpi.{Enumerable, Nameable}

object Tariffs {
  sealed abstract class DayOfWeek(val name: String) extends Nameable
  object DayOfWeek extends Enumerable[DayOfWeek] {
    case object Monday extends DayOfWeek("MONDAY")
    case object Tuesday extends DayOfWeek("TUESDAY")
    case object Wednesday extends DayOfWeek("WEDNESDAY")
    case object Thursday extends DayOfWeek("THURSDAY")
    case object Friday extends DayOfWeek("FRIDAY")
    case object Saturday extends DayOfWeek("SATURDAY")
    case object Sunday extends DayOfWeek("SUNDAY")
    val values = Set(Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
  }

  sealed abstract class TariffDimensionType(val name: String) extends Nameable
  object TariffDimensionType extends Enumerable[TariffDimensionType] {
    case object Energy extends TariffDimensionType("ENERGY")
    case object Flat extends TariffDimensionType("FLAT")
    case object ParkingTime extends TariffDimensionType("PARKING_TIME")
    case object Time extends TariffDimensionType("TIME")
    val values = Set(Energy, Flat, ParkingTime, Time)
  }

  final case class PriceComponent(
    `type`: TariffDimensionType,
    price: BigDecimal,
    stepSize: Int
  )

  final case class TariffRestrictions(
    startTime: Option[LocalTime] = None,
    endTime: Option[LocalTime] = None,
    startDate: Option[LocalDate] = None,
    endDate: Option[LocalDate] = None,
    minKwh: Option[BigDecimal] = None,
    maxKwh: Option[BigDecimal] = None,
    minPower: Option[BigDecimal] = None,
    maxPower: Option[BigDecimal] = None,
    minDuration: Option[Duration] = None,
    maxDuration: Option[Duration] = None,
    dayOfWeek: Option[Iterable[DayOfWeek]] = None
  )

  final case class TariffElement(
    priceComponents: Iterable[PriceComponent],
    restrictions: Option[TariffRestrictions] = None
  )

  final case class Tariff(
    id: String,
    currency: CurrencyCode,
    tariffAltText: Option[Iterable[DisplayText]] = None,
    tariffAltUrl: Option[Url] = None,
    elements: Iterable[TariffElement],
    energyMix: Option[EnergyMix] = None,
    lastUpdated: ZonedDateTime
  )
}
