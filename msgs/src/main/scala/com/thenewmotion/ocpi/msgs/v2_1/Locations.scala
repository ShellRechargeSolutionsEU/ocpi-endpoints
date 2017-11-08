package com.thenewmotion.ocpi
package msgs
package v2_1

import java.time.{LocalTime, ZonedDateTime}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.ResourceType.{Full, Patch}
import scala.util.{Failure, Success, Try}

object Locations {

  trait LocationId extends Any { def value: String }
  object LocationId {
    private case class LocationIdImpl(value: String) extends AnyVal with LocationId {
      override def toString: String = value
    }

    def apply(value: String): LocationId = {
      require(value.length <= 39, "Location Id must be 39 characters or less")
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
  ) extends BaseLocation[Full] {
    require(evses.nonEmpty, "Location must have at least one Evse")
  }

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

  sealed trait LocationType extends Nameable
  object LocationType extends Enumerable[LocationType] {
    case object OnStreet extends LocationType { val name = "ON_STREET" }
    case object ParkingGarage extends LocationType { val name = "PARKING_GARAGE" }
    case object UndergroundGarage extends LocationType { val name = "UNDERGROUND_GARAGE" }
    case object ParkingLot extends LocationType { val name = "PARKING_LOT" }
    case object Other extends LocationType { val name = "OTHER" }
    case object Unknown extends LocationType { val name = "UNKNOWN" }
    val values = Iterable(OnStreet, ParkingGarage, UndergroundGarage, ParkingLot, Other, Unknown)
  }

  sealed trait Facility extends Nameable
  object Facility extends Enumerable[Facility] {
    case object Hotel extends Facility { val name = "HOTEL" }
    case object Restaurant extends Facility { val name = "RESTAURANT" }
    case object Cafe extends Facility { val name = "CAFE" }
    case object Mall extends Facility { val name = "MALL" }
    case object Supermarket extends Facility { val name = "SUPERMARKET" }
    case object Sport extends Facility { val name = "SPORT" }
    case object RecreationArea extends Facility { val name = "RECREATION_AREA" }
    case object Nature extends Facility { val name = "NATURE" }
    case object Museum extends Facility { val name = "MUSEUM" }
    case object BusStop extends Facility { val name = "BUS_STOP" }
    case object TaxiStand extends Facility { val name = "TAXI_STAND" }
    case object TrainStation extends Facility { val name = "TRAIN_STATION" }
    case object Airport extends Facility { val name = "AIRPORT" }
    case object CarpoolParking extends Facility { val name = "CARPOOL_PARKING" }
    case object FuelStation extends Facility { val name = "FUEL_STATION" }
    case object Wifi extends Facility { val name = "WIFI" }
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

  sealed trait EnergySourceCategory extends Nameable
  object EnergySourceCategory extends Enumerable[EnergySourceCategory] {
    case object Nuclear extends EnergySourceCategory { val name = "NUCLEAR" }
    case object GeneralFossil extends EnergySourceCategory { val name = "GENERAL_FOSSIL" }
    case object Coal extends EnergySourceCategory { val name = "COAL" }
    case object Gas extends EnergySourceCategory { val name = "GAS" }
    case object GeneralGreen extends EnergySourceCategory { val name = "GENERAL_GREEN" }
    case object Solar extends EnergySourceCategory { val name = "SOLAR" }
    case object Wind extends EnergySourceCategory { val name = "WIND" }
    case object Water extends EnergySourceCategory { val name = "WATER" }
    val values = Seq(Nuclear, GeneralFossil, Coal, Gas, GeneralGreen, Solar, Wind, Water)
  }

  sealed trait EnvironmentalImpactCategory extends Nameable
  object EnvironmentalImpactCategory extends Enumerable[EnvironmentalImpactCategory] {
    case object NuclearWaste extends EnvironmentalImpactCategory { val name = "NUCLEAR_WASTE" }
    case object CarbonDioxide extends EnvironmentalImpactCategory { val name = "CARBON_DIOXIDE" }
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
    require(periodEnd.isAfter(periodBegin), "periodEnd must be after periodBegin")
  }

  object RegularHours {
    def apply(
      weekday: Int,
      periodBegin: String,
      periodEnd: String
    ): RegularHours = RegularHours(weekday, LocalTime.parse(periodBegin), LocalTime.parse(periodEnd))
  }

  case class ExceptionalPeriod(
    periodBegin: ZonedDateTime,
    periodEnd: ZonedDateTime
  )

  case class Hours(
    twentyfourseven: Boolean,
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

  sealed trait Capability extends Nameable
  object Capability extends Enumerable[Capability] {
    case object ChargingProfileCapable extends Capability { val name = "CHARGING_PROFILE_CAPABLE" }
    case object CreditCardPayable extends Capability { val name = "CREDIT_CARD_PAYABLE" }
    case object Reservable extends Capability { val name = "RESERVABLE" }
    case object RfidReader extends Capability { val name = "RFID_READER" }
    case object RemoteStartStopCapable extends Capability { val name = "REMOTE_START_STOP_CAPABLE" }
    case object UnlockCapabale extends Capability { val name = "UNLOCK_CAPABLE" }
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
  ) extends BaseEvse[Full] {
    type C = Connector
    require(connectors.nonEmpty, "Evse must have at least one connector")
  }

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
  ) extends BaseEvse[Patch] {
    type C = ConnectorPatch
  }

  trait Coordinate extends Any {
    def value: Double
    override def toString: String = value.toString
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

  trait Latitude extends Any with Coordinate

  object Latitude extends CoordinateCompanion[Latitude] {
    private case class LatitudeImpl(value: Double) extends AnyVal with Latitude
    override protected def create(value: Double): Latitude = LatitudeImpl(value)
    override protected val lowerLimit: Double = -90
    override protected val upperLimit: Double = 90
  }

  trait Longitude extends Any with Coordinate

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

  sealed trait ParkingRestriction extends Nameable
  object ParkingRestriction extends Enumerable[ParkingRestriction] {
    case object EvOnly extends ParkingRestriction { val name = "EV_ONLY" }
    case object Plugged extends ParkingRestriction { val name = "PLUGGED" }
    case object Disabled extends ParkingRestriction { val name = "DISABLED" }
    case object Customers extends ParkingRestriction { val name = "CUSTOMERS" }
    case object Motorcycles extends ParkingRestriction { val name = "MOTORCYCLES" }
    val values = Iterable(EvOnly, Plugged, Disabled, Customers, Motorcycles)
  }

  sealed trait ConnectorStatus extends Nameable
  object ConnectorStatus extends Enumerable[ConnectorStatus] {
    case object Available extends ConnectorStatus { val name = "AVAILABLE" }
    case object Blocked extends ConnectorStatus { val name = "BLOCKED" }
    case object Charging extends ConnectorStatus { val name = "CHARGING" }
    case object Inoperative extends ConnectorStatus { val name = "INOPERATIVE" }
    case object OutOfOrder extends ConnectorStatus { val name = "OUTOFORDER" }
    case object Planned extends ConnectorStatus { val name = "PLANNED" }
    case object Removed extends ConnectorStatus { val name = "REMOVED" }
    case object Reserved extends ConnectorStatus { val name = "RESERVED" }
    case object Unknown extends ConnectorStatus { val name = "UNKNOWN" }

    //case object Unknown extends ConnectorStatus {val name = "unknown"}
    val values = Iterable(Available, Blocked, Charging, Inoperative, OutOfOrder, Planned, Removed, Reserved, Unknown)
  }

  case class StatusSchedule(
    periodBegin: ZonedDateTime,
    periodEnd: Option[ZonedDateTime],
    status: ConnectorStatus
  )

  sealed trait ConnectorType extends Nameable

  object ConnectorType extends Enumerable[ConnectorType] {
    case object CHADEMO extends ConnectorType { val name = "CHADEMO" }
    case object `IEC_62196_T1` extends ConnectorType { val name = "IEC_62196_T1" }
    case object `IEC_62196_T1_COMBO` extends ConnectorType { val name = "IEC_62196_T1_COMBO" }
    case object `IEC_62196_T2` extends ConnectorType { val name = "IEC_62196_T2" }
    case object `IEC_62196_T2_COMBO` extends ConnectorType { val name = "IEC_62196_T2_COMBO" }
    case object `IEC_62196_T3A` extends ConnectorType { val name = "IEC_62196_T3A" }
    case object `IEC_62196_T3C` extends ConnectorType { val name = "IEC_62196_T3C" }
    case object `DOMESTIC_A` extends ConnectorType { val name = "DOMESTIC_A" }
    case object `DOMESTIC_B` extends ConnectorType { val name = "DOMESTIC_B" }
    case object `DOMESTIC_C` extends ConnectorType { val name = "DOMESTIC_C" }
    case object `DOMESTIC_D` extends ConnectorType { val name = "DOMESTIC_D" }
    case object `DOMESTIC_E` extends ConnectorType { val name = "DOMESTIC_E" }
    case object `DOMESTIC_F` extends ConnectorType { val name = "DOMESTIC_F" }
    case object `DOMESTIC_G` extends ConnectorType { val name = "DOMESTIC_G" }
    case object `DOMESTIC_H` extends ConnectorType { val name = "DOMESTIC_H" }
    case object `DOMESTIC_I` extends ConnectorType { val name = "DOMESTIC_I" }
    case object `DOMESTIC_J` extends ConnectorType { val name = "DOMESTIC_J" }
    case object `DOMESTIC_K` extends ConnectorType { val name = "DOMESTIC_K" }
    case object `DOMESTIC_L` extends ConnectorType { val name = "DOMESTIC_L" }
    case object `TESLA_R` extends ConnectorType { val name = "TESLA_R" }
    case object `TESLA_S` extends ConnectorType { val name = "TESLA_S" }
    case object `IEC_60309_2_single_16` extends ConnectorType { val name = "IEC_60309_2_single_16" }
    case object `IEC_60309_2_three_16` extends ConnectorType { val name = "IEC_60309_2_three_16" }
    case object `IEC_60309_2_three_32` extends ConnectorType { val name = "IEC_60309_2_three_32" }
    case object `IEC_60309_2_three_64` extends ConnectorType { val name = "IEC_60309_2_three_64" }
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

  sealed trait ConnectorFormat extends Nameable
  object ConnectorFormat extends Enumerable[ConnectorFormat] {
    case object Socket extends ConnectorFormat { val name = "SOCKET" }
    case object Cable extends ConnectorFormat { val name = "CABLE" }
    val values = Iterable(Socket, Cable)
  }

  sealed trait PowerType extends Nameable

  object PowerType extends Enumerable[PowerType] {
    case object AC1Phase extends PowerType { val name = "AC_1_PHASE" }
    case object AC3Phase extends PowerType { val name = "AC_3_PHASE" }
    case object DC extends PowerType { val name = "DC" }
    val values = Iterable(AC1Phase, AC3Phase, DC)
  }
}
