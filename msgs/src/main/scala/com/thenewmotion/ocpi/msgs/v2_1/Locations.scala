package com.thenewmotion.ocpi
package msgs
package v2_1

import java.time.{LocalTime, ZonedDateTime}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.ResourceType.{Full, Patch}
import scala.math.BigDecimal.RoundingMode
import scala.util.{Failure, Success, Try}

object Locations {

  trait LocationId extends Any { def value: String }
  object LocationId {
    private case class LocationIdImpl(value: String) extends AnyVal with LocationId {
      override def toString: String = value
    }

    def apply(value: String): LocationId = {
      require(value.length <= 39, "Location Id must be 39 characters or less")
      require(value.nonEmpty, "Location Id cannot be an empty string")
      LocationIdImpl(value)
    }

    def unapply(locId: LocationId): Option[String] =
      Some(locId.value)
  }

  trait BaseLocation[RT <: ResourceType] extends Resource[RT] {
    def lastUpdated: RT#F[ZonedDateTime]
    def `type`: RT#F[LocationType]
    def name: Option[String]
    def address: RT#F[String]
    def city: RT#F[String]
    def postalCode: RT#F[String]
    def country: RT#F[CountryCode]
    def coordinates: RT#F[GeoLocation]
    def relatedLocations: RT#F[Iterable[AdditionalGeoLocation]]
    def evses: RT#F[Iterable[Evse]]
    def directions: RT#F[Iterable[DisplayText]]
    def operator: Option[BusinessDetails]
    def suboperator: Option[BusinessDetails]
    def owner: Option[BusinessDetails]
    def facilities: RT#F[Iterable[Facility]]
    def timeZone: Option[String]
    def openingTimes: Option[Hours]
    def chargingWhenClosed: Option[Boolean]
    def images: RT#F[Iterable[Image]]
    def energyMix: Option[EnergyMix]
  }

  case class Location(
    id: LocationId,
    lastUpdated: ZonedDateTime,
    `type`: LocationType,
    name: Option[String],
    address: String,
    city: String,
    postalCode: String,
    country: CountryCode,
    coordinates: GeoLocation,
    relatedLocations: Iterable[AdditionalGeoLocation] = Nil,
    evses: Iterable[Evse] = Nil,
    directions: Iterable[DisplayText] = Nil,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: Iterable[Facility] = Nil,
    timeZone: Option[String] = None,
    openingTimes: Option[Hours] = None,
    chargingWhenClosed: Option[Boolean] = None,
    images: Iterable[Image] = Nil,
    energyMix: Option[EnergyMix] = None
  ) extends BaseLocation[Full]

  case class LocationPatch(
    lastUpdated: Option[ZonedDateTime] = None,
    `type`: Option[LocationType] = None,
    name: Option[String] = None,
    address: Option[String] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    country: Option[CountryCode] = None,
    coordinates: Option[GeoLocation] = None,
    relatedLocations: Option[Iterable[AdditionalGeoLocation]] = None,
    evses: Option[Iterable[Evse]] = None,
    directions: Option[Iterable[DisplayText]] = None,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: Option[Iterable[Facility]] = None,
    timeZone: Option[String] = None,
    openingTimes: Option[Hours] = None,
    chargingWhenClosed: Option[Boolean] = None,
    images: Option[Iterable[Image]] = None,
    energyMix: Option[EnergyMix] = None
  ) extends BaseLocation[Patch]

  sealed abstract class LocationType(val name: String) extends Nameable
  implicit object LocationType extends Enumerable[LocationType] {
    case object OnStreet extends LocationType("ON_STREET")
    case object ParkingGarage extends LocationType("PARKING_GARAGE")
    case object UndergroundGarage extends LocationType("UNDERGROUND_GARAGE")
    case object ParkingLot extends LocationType("PARKING_LOT")
    case object Other extends LocationType("OTHER")
    case object Unknown extends LocationType("UNKNOWN")
    val values = Iterable(OnStreet, ParkingGarage, UndergroundGarage, ParkingLot, Other, Unknown)
  }

  sealed abstract class Facility(val name: String) extends Nameable
  implicit object Facility extends Enumerable[Facility] {
    case object Hotel extends Facility("HOTEL")
    case object Restaurant extends Facility("RESTAURANT")
    case object Cafe extends Facility("CAFE")
    case object Mall extends Facility("MALL")
    case object Supermarket extends Facility("SUPERMARKET")
    case object Sport extends Facility("SPORT")
    case object RecreationArea extends Facility("RECREATION_AREA")
    case object Nature extends Facility("NATURE")
    case object Museum extends Facility("MUSEUM")
    case object BusStop extends Facility("BUS_STOP")
    case object TaxiStand extends Facility("TAXI_STAND")
    case object TrainStation extends Facility("TRAIN_STATION")
    case object Airport extends Facility("AIRPORT")
    case object CarpoolParking extends Facility("CARPOOL_PARKING")
    case object FuelStation extends Facility("FUEL_STATION")
    case object Wifi extends Facility("WIFI")
    val values = Seq(
      Hotel,
      Restaurant,
      Cafe,
      Mall,
      Supermarket,
      Sport,
      RecreationArea,
      Nature,
      Museum,
      BusStop,
      TaxiStand,
      TrainStation,
      Airport,
      CarpoolParking,
      FuelStation,
      Wifi
    )
  }

  sealed abstract class EnergySourceCategory(val name: String) extends Nameable
  implicit object EnergySourceCategory extends Enumerable[EnergySourceCategory] {
    case object Nuclear extends EnergySourceCategory("NUCLEAR")
    case object GeneralFossil extends EnergySourceCategory("GENERAL_FOSSIL")
    case object Coal extends EnergySourceCategory("COAL")
    case object Gas extends EnergySourceCategory("GAS")
    case object GeneralGreen extends EnergySourceCategory("GENERAL_GREEN")
    case object Solar extends EnergySourceCategory("SOLAR")
    case object Wind extends EnergySourceCategory("WIND")
    case object Water extends EnergySourceCategory("WATER")
    val values = Seq(Nuclear, GeneralFossil, Coal, Gas, GeneralGreen, Solar, Wind, Water)
  }

  sealed abstract class EnvironmentalImpactCategory(val name: String) extends Nameable
  implicit object EnvironmentalImpactCategory extends Enumerable[EnvironmentalImpactCategory] {
    case object NuclearWaste extends EnvironmentalImpactCategory("NUCLEAR_WASTE")
    case object CarbonDioxide extends EnvironmentalImpactCategory("CARBON_DIOXIDE")
    val values = Seq(NuclearWaste, CarbonDioxide)
  }

  case class EnergySource(
    source: EnergySourceCategory,
    percentage: Double
  )

  case class EnvironmentalImpact(
    source: EnvironmentalImpactCategory,
    percentage: Double
  )

  case class EnergyMix(
    isGreenEnergy: Boolean,
    energySources: Iterable[EnergySource] = Nil,
    environImpact: Iterable[EnvironmentalImpact] = Nil,
    supplierName: Option[String] = None,
    energyProductName: Option[String] = None
  )

  case class RegularHours(
    weekday: Int,
    periodBegin: LocalTime,
    periodEnd: LocalTime
  ) {
    private def realPeriodEnd = periodEnd match {
      case p if p == LocalTime.MIN => LocalTime.MAX  // If 00:00, validate against 23:59:59.999999999 instead
      case p => p
    }

    require(realPeriodEnd.isAfter(periodBegin), s"period_end ($periodEnd) must be after period_begin ($periodBegin")
  }

  object RegularHours {
    def apply(
      weekday: Int,
      periodBegin: String,
      periodEnd: String
    ): RegularHours = RegularHours(weekday, LocalTimeParser.parse(periodBegin), LocalTimeParser.parse(periodEnd))
  }

  case class ExceptionalPeriod(
    periodBegin: ZonedDateTime,
    periodEnd: ZonedDateTime
  )

  case class Hours(
    twentyfourseven: Boolean = false,
    regularHours: Iterable[RegularHours] = Nil,
    exceptionalOpenings: Iterable[ExceptionalPeriod] = Nil,
    exceptionalClosings: Iterable[ExceptionalPeriod] = Nil
  )

  trait ConnectorId extends Any { def value: String }
  object ConnectorId {
    private case class ConnectorIdImpl(value: String) extends AnyVal with ConnectorId {
      override def toString: String = value
    }

    def apply(value: String): ConnectorId = {
      require(value.length <= 36, "Connector Id must be 36 characters or less")
      require(value.nonEmpty, "Token Id cannot be an empty string")
      ConnectorIdImpl(value)
    }

    def unapply(conId: ConnectorId): Option[String] =
      Some(conId.value)
  }

  trait BaseConnector[RT <: ResourceType] extends Resource[RT] {
    def lastUpdated: RT#F[ZonedDateTime]
    def standard: RT#F[ConnectorType]
    def format: RT#F[ConnectorFormat]
    def powerType: RT#F[PowerType]
    def voltage: RT#F[Int]
    def amperage: RT#F[Int]
    def tariffId: Option[String]
    def termsAndConditions: Option[Url]
  }

  case class Connector(
    id: ConnectorId,
    lastUpdated: ZonedDateTime,
    standard: ConnectorType,
    format: ConnectorFormat,
    powerType: PowerType,
    voltage: Int,
    amperage: Int,
    tariffId: Option[String],
    termsAndConditions: Option[Url] = None
  ) extends BaseConnector[Full]

  case class ConnectorPatch(
    lastUpdated: Option[ZonedDateTime] = None,
    standard: Option[ConnectorType] = None,
    format: Option[ConnectorFormat] = None,
    powerType: Option[PowerType] = None,
    voltage: Option[Int] = None,
    amperage: Option[Int] = None,
    tariffId: Option[String] = None,
    termsAndConditions: Option[Url] = None
  ) extends BaseConnector[Patch]

  sealed abstract class Capability(val name: String) extends Nameable
  implicit object Capability extends Enumerable[Capability] {
    case object ChargingProfileCapable extends Capability("CHARGING_PROFILE_CAPABLE")
    case object CreditCardPayable extends Capability("CREDIT_CARD_PAYABLE")
    case object Reservable extends Capability("RESERVABLE")
    case object RfidReader extends Capability("RFID_READER")
    case object RemoteStartStopCapable extends Capability("REMOTE_START_STOP_CAPABLE")
    case object UnlockCapabale extends Capability("UNLOCK_CAPABLE")
    val values = Iterable(
      ChargingProfileCapable,
      CreditCardPayable,
      Reservable,
      RfidReader,
      RemoteStartStopCapable,
      UnlockCapabale
    )
  }

  trait EvseUid extends Any { def value: String }
  object EvseUid {
    private case class EvseUidImpl(value: String) extends AnyVal with EvseUid {
      override def toString: String = value
    }

    def apply(value: String): EvseUid = {
      require(value.length <= 39, "Evse Uid must be 39 characters or less")
      require(value.nonEmpty, "Token Id cannot be an empty string")
      EvseUidImpl(value)
    }

    def unapply(evseUid: EvseUid): Option[String] =
      Some(evseUid.value)
  }

  trait BaseEvse[RT <: ResourceType] extends Resource[RT] {
    def lastUpdated: RT#F[ZonedDateTime]
    def status: RT#F[ConnectorStatus]
    def connectors: RT#F[Iterable[Connector]]
    def statusSchedule: RT#F[Iterable[StatusSchedule]]
    def capabilities: RT#F[Iterable[Capability]]
    def evseId: Option[String]
    def floorLevel: Option[String]
    def coordinates: Option[GeoLocation]
    def physicalReference: Option[String]
    def directions: RT#F[Iterable[DisplayText]]
    def parkingRestrictions: RT#F[Iterable[ParkingRestriction]]
    def images: RT#F[Iterable[Image]]
  }

  case class Evse(
    uid: EvseUid,
    lastUpdated: ZonedDateTime,
    status: ConnectorStatus,
    connectors: Iterable[Connector],
    statusSchedule: Iterable[StatusSchedule] = Nil,
    capabilities: Iterable[Capability] = Nil,
    evseId: Option[String] = None,
    floorLevel: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    physicalReference: Option[String] = None,
    directions: Iterable[DisplayText] = Nil,
    parkingRestrictions: Iterable[ParkingRestriction] = Nil,
    images: Iterable[Image] = Nil
  ) extends BaseEvse[Full]

  case class EvsePatch(
    lastUpdated: Option[ZonedDateTime] = None,
    status: Option[ConnectorStatus] = None,
    connectors: Option[Iterable[Connector]] = None,
    statusSchedule: Option[Iterable[StatusSchedule]] = None,
    capabilities: Option[Iterable[Capability]] = None,
    evseId: Option[String] = None,
    floorLevel: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    physicalReference: Option[String] = None,
    directions: Option[Iterable[DisplayText]] = None,
    parkingRestrictions: Option[Iterable[ParkingRestriction]] = None,
    images: Option[Iterable[Image]] = None
  ) extends BaseEvse[Patch]

  sealed trait Coordinate extends Any {
    def value: Double
    override def toString: String = BigDecimal(value).setScale(6, RoundingMode.HALF_UP).toString
  }

  trait CoordinateCompanion[T <: Coordinate] {
    protected def create(value: Double): T
    protected def lowerLimit: Double
    protected def upperLimit: Double

    private def internalApply(
      value: Double,
      strict: Boolean
    ): T = {
      val rounded = math.rint(value * 1000000) / 1000000
      require(rounded >= lowerLimit && rounded <= upperLimit, s"must be between $lowerLimit and $upperLimit")
      require(!strict || value == rounded, "must have a precision of 6 or less")
      create(rounded)
    }

    private def internalApply(
      value: String,
      strict: Boolean
    ): T =
      Try(value.toDouble) match {
        case Failure(ex: NumberFormatException) =>
          throw new IllegalArgumentException(s"$value is not a valid number", ex)
        case Failure(ex) => throw ex
        case Success(x)  => internalApply(x, strict = strict)
      }

    def apply(value: Double): T = internalApply(value, strict = false)

    def apply(value: String): T = internalApply(value, strict = false)

    def strict(value: Double): T = internalApply(value, strict = true)

    def strict(value: String): T = internalApply(value, strict = true)

    def unapply(value: T): Option[Double] = Some(value.value)
  }

  sealed trait Latitude extends Any with Coordinate

  object Latitude extends CoordinateCompanion[Latitude] {
    private case class LatitudeImpl(value: Double) extends AnyVal with Latitude
    override protected def create(value: Double): Latitude = LatitudeImpl(value)
    override protected val lowerLimit: Double = -90
    override protected val upperLimit: Double = 90
  }

  sealed trait Longitude extends Any with Coordinate

  object Longitude extends CoordinateCompanion[Longitude] {
    private case class LongitudeImpl(value: Double) extends AnyVal with Longitude
    override def create(value: Double): Longitude = LongitudeImpl(value)
    override protected val lowerLimit: Double = -180
    override protected val upperLimit: Double = 180
  }

  case class AdditionalGeoLocation(
    latitude: Latitude,
    longitude: Longitude,
    name: Option[DisplayText] = None
  )

  case class GeoLocation(
    latitude: Latitude,
    longitude: Longitude
  )

  sealed abstract class ParkingRestriction(val name: String) extends Nameable
  implicit object ParkingRestriction extends Enumerable[ParkingRestriction] {
    case object EvOnly extends ParkingRestriction("EV_ONLY")
    case object Plugged extends ParkingRestriction("PLUGGED")
    case object Disabled extends ParkingRestriction("DISABLED")
    case object Customers extends ParkingRestriction("CUSTOMERS")
    case object Motorcycles extends ParkingRestriction("MOTORCYCLES")
    val values = Iterable(EvOnly, Plugged, Disabled, Customers, Motorcycles)
  }

  sealed abstract class ConnectorStatus(val name: String) extends Nameable
  implicit object ConnectorStatus extends Enumerable[ConnectorStatus] {
    case object Available extends ConnectorStatus("AVAILABLE")
    case object Blocked extends ConnectorStatus("BLOCKED")
    case object Charging extends ConnectorStatus("CHARGING")
    case object Inoperative extends ConnectorStatus("INOPERATIVE")
    case object OutOfOrder extends ConnectorStatus("OUTOFORDER")
    case object Planned extends ConnectorStatus("PLANNED")
    case object Removed extends ConnectorStatus("REMOVED")
    case object Reserved extends ConnectorStatus("RESERVED")
    case object Unknown extends ConnectorStatus("UNKNOWN")
    val values = Iterable(Available, Blocked, Charging, Inoperative, OutOfOrder, Planned, Removed, Reserved, Unknown)
  }

  case class StatusSchedule(
    periodBegin: ZonedDateTime,
    periodEnd: Option[ZonedDateTime],
    status: ConnectorStatus
  )

  sealed abstract class ConnectorType(val name: String) extends Nameable

  implicit object ConnectorType extends Enumerable[ConnectorType] {
    case object CHADEMO extends ConnectorType("CHADEMO")
    case object `IEC_62196_T1` extends ConnectorType("IEC_62196_T1")
    case object `IEC_62196_T1_COMBO` extends ConnectorType("IEC_62196_T1_COMBO")
    case object `IEC_62196_T2` extends ConnectorType("IEC_62196_T2")
    case object `IEC_62196_T2_COMBO` extends ConnectorType("IEC_62196_T2_COMBO")
    case object `IEC_62196_T3A` extends ConnectorType("IEC_62196_T3A")
    case object `IEC_62196_T3C` extends ConnectorType("IEC_62196_T3C")
    case object `DOMESTIC_A` extends ConnectorType("DOMESTIC_A")
    case object `DOMESTIC_B` extends ConnectorType("DOMESTIC_B")
    case object `DOMESTIC_C` extends ConnectorType("DOMESTIC_C")
    case object `DOMESTIC_D` extends ConnectorType("DOMESTIC_D")
    case object `DOMESTIC_E` extends ConnectorType("DOMESTIC_E")
    case object `DOMESTIC_F` extends ConnectorType("DOMESTIC_F")
    case object `DOMESTIC_G` extends ConnectorType("DOMESTIC_G")
    case object `DOMESTIC_H` extends ConnectorType("DOMESTIC_H")
    case object `DOMESTIC_I` extends ConnectorType("DOMESTIC_I")
    case object `DOMESTIC_J` extends ConnectorType("DOMESTIC_J")
    case object `DOMESTIC_K` extends ConnectorType("DOMESTIC_K")
    case object `DOMESTIC_L` extends ConnectorType("DOMESTIC_L")
    case object `TESLA_R` extends ConnectorType("TESLA_R")
    case object `TESLA_S` extends ConnectorType("TESLA_S")
    case object `IEC_60309_2_single_16` extends ConnectorType("IEC_60309_2_single_16")
    case object `IEC_60309_2_three_16` extends ConnectorType("IEC_60309_2_three_16")
    case object `IEC_60309_2_three_32` extends ConnectorType("IEC_60309_2_three_32")
    case object `IEC_60309_2_three_64` extends ConnectorType("IEC_60309_2_three_64")
    val values = Iterable(
      CHADEMO,
      `IEC_62196_T1`,
      `IEC_62196_T1_COMBO`,
      `IEC_62196_T2`,
      `IEC_62196_T2_COMBO`,
      `IEC_62196_T3A`,
      `IEC_62196_T3C`,
      `DOMESTIC_A`,
      `DOMESTIC_B`,
      `DOMESTIC_C`,
      `DOMESTIC_D`,
      `DOMESTIC_E`,
      `DOMESTIC_F`,
      `DOMESTIC_G`,
      `DOMESTIC_H`,
      `DOMESTIC_I`,
      `DOMESTIC_J`,
      `DOMESTIC_K`,
      `DOMESTIC_L`,
      `TESLA_R`,
      `TESLA_S`,
      `IEC_60309_2_single_16`,
      `IEC_60309_2_three_16`,
      `IEC_60309_2_three_32`,
      `IEC_60309_2_three_64`
    )
  }

  sealed abstract class ConnectorFormat(val name: String) extends Nameable
  implicit object ConnectorFormat extends Enumerable[ConnectorFormat] {
    case object Socket extends ConnectorFormat("SOCKET")
    case object Cable extends ConnectorFormat("CABLE")
    val values = Iterable(Socket, Cable)
  }

  sealed abstract class PowerType(val name: String) extends Nameable

  implicit object PowerType extends Enumerable[PowerType] {
    case object AC1Phase extends PowerType("AC_1_PHASE")
    case object AC3Phase extends PowerType("AC_3_PHASE")
    case object DC extends PowerType("DC")
    val values = Iterable(AC1Phase, AC3Phase, DC)
  }
}
