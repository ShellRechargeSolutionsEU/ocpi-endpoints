package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import org.joda.time.DateTime

object Locations {

  case class Location(
    id: String,
    lastUpdated: DateTime,
    `type`:	LocationType,
    name:	Option[String],
    address: String,
    city:	String,
    postalCode: String,
    country:	String,
    coordinates:	GeoLocation,
    relatedLocations: Iterable[AdditionalGeoLocation] = Nil,
    evses: Iterable[Evse] = Nil,
    directions:	Iterable[DisplayText] = Nil,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: Iterable[Facility] = Nil,
    timeZone: Option[String] = None,
    openingTimes: Option[Hours] = None,
    chargingWhenClosed: Option[Boolean] = Some(true),
    images: Iterable[Image] = Nil,
    energyMix: Option[EnergyMix] = None) {
    require(country.length == 3, "Location needs 3-letter, ISO 3166-1 country code!")
  }

  case class LocationPatch(
    id: Option[String] = None,
    lastUpdated: Option[DateTime] = None,
    `type`: Option[LocationType] = None,
    name: Option[String] = None,
    address: Option[String] = None,
    city: Option[String] = None,
    postalCode: Option[String] = None,
    country: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    relatedLocations: Option[Iterable[AdditionalGeoLocation]] = None,
    evses: Option[Iterable[Evse]] = None,
    directions: Option[DisplayText] = None,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: Option[Iterable[Facility]] = None,
    timeZone: Option[String] = None,
    openingTimes: Option[Hours] = None,
    chargingWhenClosed: Option[Boolean] = None,
    images: Option[Iterable[Image]] = None,
    energyMix: Option[EnergyMix] = None) {
    require(country.fold(true)(_.length == 3), "Location needs 3-letter, ISO 3166-1 country code!")
  }

  sealed trait LocationType extends Nameable
  object LocationType extends Enumerable[LocationType] {
    case object OnStreet extends LocationType {val name = "ON_STREET"}
    case object ParkingGarage extends LocationType {val name = "PARKING_GARAGE"}
    case object UndergroundGarage extends LocationType {val name = "UNDERGROUND_GARAGE"}
    case object ParkingLot extends LocationType {val name = "PARKING_LOT"}
    case object Other extends LocationType {val name = "OTHER"}
    case object Unknown extends LocationType {val name = "UNKNOWN"}
    val values = Iterable(OnStreet, ParkingGarage, UndergroundGarage,
      ParkingLot, Other, Unknown)
  }

  sealed trait Facility extends Nameable
  object Facility extends Enumerable[Facility] {
    case object Hotel extends Facility {val name = "HOTEL"}
    case object Restaurant extends Facility {val name = "RESTAURANT"}
    case object Cafe extends Facility {val name = "CAFE"}
    case object Mall extends Facility {val name = "MALL"}
    case object Supermarket extends Facility {val name = "SUPERMARKET"}
    case object Sport extends Facility {val name = "SPORT"}
    case object RecreationArea extends Facility {val name = "RECREATION_AREA"}
    case object Nature extends Facility {val name = "NATURE"}
    case object Museum extends Facility {val name = "MUSEUM"}
    case object BusStop extends Facility {val name = "BUS_STOP"}
    case object TaxiStand extends Facility {val name = "TAXI_STAND"}
    case object TrainStation extends Facility {val name = "TRAIN_STATION"}
    case object Airport extends Facility {val name = "AIRPORT"}
    case object CarpoolParking extends Facility {val name = "CARPOOL_PARKING"}
    case object FuelStation extends Facility {val name = "FUEL_STATION"}
    case object Wifi extends Facility {val name = "WIFI"}
    val values = Seq(Hotel, Restaurant, Cafe, Mall, Supermarket, Sport, RecreationArea, Nature, Museum, BusStop,
      TaxiStand, TrainStation, Airport, CarpoolParking, FuelStation, Wifi)
  }

  sealed trait EnergySourceCategory extends Nameable
  object EnergySourceCategory extends Enumerable[EnergySourceCategory] {
    case object Nuclear extends EnergySourceCategory {val name = "NUCLEAR"}
    case object GeneralFossil extends EnergySourceCategory {val name = "GENERAL_FOSSIL"}
    case object Coal extends EnergySourceCategory {val name = "COAL"}
    case object Gas extends EnergySourceCategory {val name = "GAS"}
    case object GeneralGreen extends EnergySourceCategory {val name = "GENERAL_GREEN"}
    case object Solar extends EnergySourceCategory {val name = "SOLAR"}
    case object Wind extends EnergySourceCategory {val name = "WIND"}
    case object Water extends EnergySourceCategory {val name = "WATER"}
    val values = Seq(Nuclear, GeneralFossil, Coal, Gas, GeneralGreen, Solar, Wind, Water)
  }

  sealed trait EnvironmentalImpactCategory extends Nameable
  object EnvironmentalImpactCategory extends Enumerable[EnvironmentalImpactCategory] {
    case object NuclearWaste extends EnvironmentalImpactCategory {val name = "NUCLEAR_WASTE"}
    case object CarbonDioxide extends EnvironmentalImpactCategory {val name = "CARBON_DIOXIDE"}
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
    periodBegin: String,
    periodEnd: String
  )

  case class ExceptionalPeriod(
    periodBegin: DateTime,
    periodEnd: DateTime
  )

  case class Hours(
    twentyfourseven: Boolean,
    regularHours: Iterable[RegularHours] = Nil,
    exceptionalOpenings: Iterable[ExceptionalPeriod] = Nil,
    exceptionalClosings: Iterable[ExceptionalPeriod] = Nil
  ) {
    require(regularHours.nonEmpty && !twentyfourseven
      || regularHours.isEmpty && twentyfourseven,
      "Opening hours need to be either 24/7 or have non-empty regular_hours")
  }


  case class Operator(
    identifier: Option[String], // unique identifier of the operator
    phone: Option[String],
    url: Option[String]
    )

  case class Power(
    current: Option[PowerType],
    amperage: Int,
    voltage: Int
    )

  sealed trait PricingUnit extends Nameable

  object PricingUnit extends Enumerable[PricingUnit] {
    case object Session extends PricingUnit {val name = "session"}
    case object KWhToEV extends PricingUnit {val name = "kwhtoev"}
    case object OccupancyHours extends PricingUnit {val name = "occupancyhours"}
    case object ChargingHours extends PricingUnit {val name = "charginghours"}
    case object IdleHours extends PricingUnit {val name = "idlehours"}
    val values = Iterable(Session, KWhToEV, OccupancyHours, ChargingHours, IdleHours)
  }

  sealed trait PeriodType extends Nameable

  object PeriodType extends Enumerable[PeriodType] {
    case object Charging extends PeriodType {val name = "Charging"}
    case object Parking extends PeriodType {val name = "Parking"}
    val values = Iterable(Charging, Parking)
  }

  case class Connector(
    id: String,
    lastUpdated: DateTime,
    standard: ConnectorType,
    format: ConnectorFormat,
    powerType:	PowerType,
    voltage: Int,
    amperage: Int,
    tariffId: Option[String],
    termsAndConditions: Option[Url] = None
  )

  case class ConnectorPatch(
    id: Option[String] = None,
    standard: Option[ConnectorType] = None,
    format: Option[ConnectorFormat] = None,
    powerType:	Option[PowerType] = None,
    voltage: Option[Int] = None,
    amperage: Option[Int] = None,
    tariffId: Option[String] = None,
    termsAndConditions: Option[Url] = None
    )

  sealed trait Capability extends Nameable
  object Capability extends Enumerable[Capability]{
    case object ChargingProfileCapable extends Capability {val name = "CHARGING_PROFILE_CAPABLE"}
    case object CreditCardPayable extends Capability {val name = "CREDIT_CARD_PAYABLE"}
    case object Reservable extends Capability {val name = "RESERVABLE"}
    case object RfidReader extends Capability {val name = "RFID_READER"}
    case object RemoteStartStopCapable extends Capability {val name = "REMOTE_START_STOP_CAPABLE"}
    case object UnlockCapabale extends Capability {val name = "UNLOCK_CAPABLE"}
    val values = Iterable(ChargingProfileCapable, CreditCardPayable, Reservable, RfidReader, RemoteStartStopCapable, UnlockCapabale)
  }

  case class Evse(
    uid: String,
    lastUpdated: DateTime,
    status: ConnectorStatus,
    connectors: Iterable[Connector],
    statusSchedule: Iterable[StatusSchedule] = Nil,
    capabilities: Iterable[Capability] = Nil,
    evseId: Option[String] = None,
    floorLevel:	Option[String] = None,
    coordinates:	Option[GeoLocation] = None,
    physicalReference:	Option[String] = None,
    directions: Iterable[DisplayText] = Nil,
    parkingRestrictions:	Iterable[ParkingRestriction] = Nil,
    images: Iterable[Image] = Nil
    ){
    require(connectors.nonEmpty, "Iterable of connector can't be empty!")
  }

  case class EvsePatch(
    uid: Option[String] = None,
    status: Option[ConnectorStatus] = None,
    connectors: Option[Iterable[Connector]] = None,
    status_schedule: Option[Iterable[StatusSchedule]] = None,
    capabilities: Option[Iterable[Capability]] = None,
    evseId: Option[String] = None,
    floorLevel: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    physicalReference: Option[String] = None,
    directions: Option[Iterable[DisplayText]] = None,
    parkingRestrictions: Option[Iterable[ParkingRestriction]] = None,
    images: Option[Iterable[Image]] = None
  )

  val LatRegex = """-?[0-9]{1,2}\.[0-9]{1,6}"""
  val LonRegex = """-?[0-9]{1,3}\.[0-9]{1,6}"""

  case class AdditionalGeoLocation(
    latitude: String,
    longitude: String,
    name: Option[DisplayText] = None
  ){
    // we will suspend this hard validation until we have a way to continue parsing
    // the Iterable even if individual locations cannot be deserialized
//    require(latitude.matches(LatRegex), s"latitude needs to conform to $LatRegex but was $latitude")
//    require(longitude.matches(LonRegex), s"longitude needs to conform to $LonRegex but was $longitude")
  }

  case class GeoLocation(
    latitude: String,
    longitude: String
    ){
    // we will suspend this hard validation until we have a way to continue parsing
    // the Iterable even if individual locations cannot be deserialized
//    require(latitude.matches(LatRegex), s"latitude needs to conform to $LatRegex but was $latitude")
//    require(longitude.matches(LonRegex), s"longitude needs to conform to $LonRegex but was $longitude")
  }

  sealed trait ParkingRestriction extends Nameable
  object ParkingRestriction extends Enumerable[ParkingRestriction] {
    case object EvOnly extends ParkingRestriction {val name = "EV_ONLY"}
    case object Plugged extends ParkingRestriction {val name = "PLUGGED"}
    case object Disabled extends ParkingRestriction {val name = "DISABLED"}
    case object Customers extends ParkingRestriction {val name = "CUSTOMERS"}
    case object Motorcycles extends ParkingRestriction {val name = "MOTORCYCLES"}
    val values = Iterable(EvOnly, Plugged, Disabled, Customers, Motorcycles)
  }

  sealed trait ConnectorStatus extends Nameable
  object ConnectorStatus extends Enumerable[ConnectorStatus] {
    case object Available extends ConnectorStatus {val name = "AVAILABLE"}
    case object Blocked extends ConnectorStatus {val name = "BLOCKED"}
    case object Charging extends ConnectorStatus {val name = "CHARGING"}
    case object Inoperative extends ConnectorStatus {val name = "INOPERATIVE"}
    case object OutOfOrder extends ConnectorStatus {val name = "OUTOFORDER"}
    case object Planned extends ConnectorStatus {val name = "PLANNED"}
    case object Removed extends ConnectorStatus {val name = "REMOVED"}
    case object Reserved extends ConnectorStatus {val name = "RESERVED"}
    case object Unknown extends ConnectorStatus {val name = "UNKNOWN"}


    //case object Unknown extends ConnectorStatus {val name = "unknown"}
    val values = Iterable(Available, Blocked, Charging, Inoperative, OutOfOrder, Planned, Removed, Reserved, Unknown)
  }

  case class StatusSchedule(
    periodBegin: DateTime,
    periodEnd: Option[DateTime],
    status: ConnectorStatus
  )

  sealed trait ConnectorType extends Nameable

  object ConnectorType extends Enumerable[ConnectorType] {
    case object	CHADEMO	extends ConnectorType {val name = "CHADEMO"}
    case object	`IEC_62196_T1`	extends ConnectorType {val name = "IEC_62196_T1"}
    case object	`IEC_62196_T1_COMBO`	extends ConnectorType {val name = "IEC_62196_T1_COMBO"}
    case object	`IEC_62196_T2`	extends ConnectorType {val name = "IEC_62196_T2"}
    case object	`IEC_62196_T2_COMBO`	extends ConnectorType {val name = "IEC_62196_T2_COMBO"}
    case object	`IEC_62196_T3A`	extends ConnectorType {val name = "IEC_62196_T3A"}
    case object	`IEC_62196_T3C`	extends ConnectorType {val name = "IEC_62196_T3C"}
    case object	`DOMESTIC_A`	extends ConnectorType {val name = "DOMESTIC_A"}
    case object	`DOMESTIC_B`	extends ConnectorType {val name = "DOMESTIC_B"}
    case object	`DOMESTIC_C`	extends ConnectorType {val name = "DOMESTIC_C"}
    case object	`DOMESTIC_D`	extends ConnectorType {val name = "DOMESTIC_D"}
    case object	`DOMESTIC_E`	extends ConnectorType {val name = "DOMESTIC_E"}
    case object	`DOMESTIC_F`	extends ConnectorType {val name = "DOMESTIC_F"}
    case object	`DOMESTIC_G`	extends ConnectorType {val name = "DOMESTIC_G"}
    case object	`DOMESTIC_H`	extends ConnectorType {val name = "DOMESTIC_H"}
    case object	`DOMESTIC_I`	extends ConnectorType {val name = "DOMESTIC_I"}
    case object	`DOMESTIC_J`	extends ConnectorType {val name = "DOMESTIC_J"}
    case object	`DOMESTIC_K`	extends ConnectorType {val name = "DOMESTIC_K"}
    case object	`DOMESTIC_L`	extends ConnectorType {val name = "DOMESTIC_L"}
    case object	`TESLA_R`	extends ConnectorType {val name = "TESLA_R"}
    case object	`TESLA_S`	extends ConnectorType {val name = "TESLA_S"}
    case object	`IEC_60309_2_single_16`	extends ConnectorType {val name = "IEC_60309_2_single_16"}
    case object	`IEC_60309_2_three_16`	extends ConnectorType {val name = "IEC_60309_2_three_16"}
    case object	`IEC_60309_2_three_32`	extends ConnectorType {val name = "IEC_60309_2_three_32"}
    case object	`IEC_60309_2_three_64`	extends ConnectorType {val name = "IEC_60309_2_three_64"}
    val values = Iterable(CHADEMO, `IEC_62196_T1`, `IEC_62196_T1_COMBO`, `IEC_62196_T2`,
      `IEC_62196_T2_COMBO`, `IEC_62196_T3A`, `IEC_62196_T3C`, `DOMESTIC_A`, `DOMESTIC_B`,
      `DOMESTIC_C`, `DOMESTIC_D`, `DOMESTIC_E`, `DOMESTIC_F`, `DOMESTIC_G`, `DOMESTIC_H`,
      `DOMESTIC_I`, `DOMESTIC_J`, `DOMESTIC_K`, `DOMESTIC_L`, `TESLA_R`, `TESLA_S`,
      `IEC_60309_2_single_16`, `IEC_60309_2_three_16`, `IEC_60309_2_three_32`, `IEC_60309_2_three_64`)
  }

  sealed trait ConnectorFormat extends Nameable
  object ConnectorFormat extends Enumerable[ConnectorFormat] {
    case object Socket extends ConnectorFormat {val name = "SOCKET"}
    case object Cable extends ConnectorFormat {val name = "CABLE"}
    val values = Iterable(Socket, Cable)
  }

  sealed trait PowerType extends Nameable

  object PowerType extends Enumerable[PowerType] {
    case object AC1Phase extends PowerType {val name = "AC_1_PHASE"}
    case object AC3Phase extends PowerType {val name = "AC_3_PHASE"}
    case object DC extends PowerType {val name = "DC"}
    val values = Iterable(AC1Phase, AC3Phase, DC)
  }
}
