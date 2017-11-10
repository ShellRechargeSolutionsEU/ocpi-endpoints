package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{Duration, LocalDate, LocalTime, ZonedDateTime}

import CommonTypes.DisplayText
import Locations.EnergyMix
import com.thenewmotion.ocpi.msgs.ResourceType.Full
import com.thenewmotion.ocpi.msgs.{CurrencyCode, Resource, Url}
import com.thenewmotion.ocpi.{Enumerable, Nameable}

object Tariffs {
  sealed abstract class DayOfWeek(val name: String) extends Nameable
  implicit object DayOfWeek extends Enumerable[DayOfWeek] {
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
  implicit object TariffDimensionType extends Enumerable[TariffDimensionType] {
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

  trait TariffId extends Any { def value: String }
  object TariffId {
    private case class TariffIdImpl(value: String) extends AnyVal with TariffId {
      override def toString: String = value
    }

    def apply(value: String): TariffId = {
      require(value.length <= 36, "Tariff Id must be 36 characters or less")
      require(value.nonEmpty, "Tariff Id cannot be an empty string")
      TariffIdImpl(value)
    }

    def unapply(conId: TariffId): Option[String] =
      Some(conId.value)
  }

  final case class Tariff(
    id: TariffId,
    currency: CurrencyCode,
    tariffAltText: Option[Iterable[DisplayText]] = None,
    tariffAltUrl: Option[Url] = None,
    elements: Iterable[TariffElement],
    energyMix: Option[EnergyMix] = None,
    lastUpdated: ZonedDateTime
  ) extends Resource[Full]
}
