package com.thenewmotion.ocpi
package msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import org.joda.time.DateTime


object Locations {

  case class Location(
    id: String,
    last_updated: DateTime,
    `type`:	LocationType,
    name:	Option[String],
    address: String,
    city:	String,
    postal_code: String,
    country:	String,
    coordinates:	GeoLocation,
    related_locations: List[AdditionalGeoLocation] = Nil,
    evses: List[Evse] = Nil,
    directions:	List[DisplayText] = Nil,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: List[Facility] = Nil,
    time_zone: Option[String] = None,
    opening_times: Option[Hours] = None,
    charging_when_closed: Option[Boolean] = Some(true),
    images: List[Image] = Nil,
    energy_mix: Option[EnergyMix] = None) {
    require(country.length == 3, "Location needs 3-letter, ISO 3166-1 country code!")
  }

  case class LocationPatch(
    id: String,
    `type`: Option[LocationType] = None,
    name: Option[String] = None,
    address: Option[String] = None,
    city: Option[String] = None,
    postal_code: Option[String] = None,
    country: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    related_locations: Option[List[AdditionalGeoLocation]] = None,
    evses: Option[List[Evse]] = None,
    directions: Option[String] = None,
    operator: Option[BusinessDetails] = None,
    suboperator: Option[BusinessDetails] = None,
    owner: Option[BusinessDetails] = None,
    facilities: Option[List[Facility]] = None,
    time_zone: Option[String] = None,
    opening_times: Option[Hours] = None,
    charging_when_closed: Option[Boolean] = None,
    images: Option[List[Image]] = None,
    energy_mix: Option[EnergyMix] = None) {
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
    val values = List(OnStreet, ParkingGarage, UndergroundGarage,
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
    is_green_energy: Boolean,
    energy_sources: List[EnergySource] = Nil,
    environ_impace: List[EnvironmentalImpact] = Nil,
    supplier_name: Option[String] = None,
    energy_product_name: Option[String] = None
  )

  case class RegularHours(
    weekday: Int,
    period_begin: String,
    period_end: String
  )

  case class ExceptionalPeriod(
    period_begin: DateTime,
    period_end: DateTime
  )

  case class Hours(
    twentyfourseven: Boolean,
    regular_hours: List[RegularHours] = Nil,
    exceptional_openings: List[ExceptionalPeriod] = Nil,
    exceptional_closings: List[ExceptionalPeriod] = Nil
  ) {
    require(regular_hours.nonEmpty && !twentyfourseven
      || regular_hours.isEmpty && twentyfourseven,
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
    val values = List(Session, KWhToEV, OccupancyHours, ChargingHours, IdleHours)
  }

  sealed trait PeriodType extends Nameable

  object PeriodType extends Enumerable[PeriodType] {
    case object Charging extends PeriodType {val name = "Charging"}
    case object Parking extends PeriodType {val name = "Parking"}
    val values = List(Charging, Parking)
  }

  case class Connector(
    id: String,
    last_updated: DateTime,
    standard: ConnectorType,
    format: ConnectorFormat,
    power_type:	PowerType,
    voltage: Int,
    amperage: Int,
    tariff_id: Option[String],
    terms_and_conditions: Option[Url] = None
  )

  case class ConnectorPatch(
    id: String,
    standard: Option[ConnectorType] = None,
    format: Option[ConnectorFormat] = None,
    power_type:	Option[PowerType] = None,
    voltage: Option[Int] = None,
    amperage: Option[Int] = None,
    tariff_id: Option[String] = None,
    terms_and_conditions: Option[Url] = None
    )



  sealed trait Capability extends Nameable
  object Capability extends Enumerable[Capability]{
    case object ChargingProfileCapable extends Capability {val name = "CHARGING_PROFILE_CAPABLE"}
    case object CreditCardPayable extends Capability {val name = "CREDIT_CARD_PAYABLE"}
    case object Reservable extends Capability {val name = "RESERVABLE"}
    case object RfidReader extends Capability {val name = "RFID_READER"}
    case object RemoteStartStopCapable extends Capability {val name = "REMOTE_START_STOP_CAPABLE"}
    case object UnlockCapabale extends Capability {val name = "UNLOCK_CAPABLE"}
    val values = List(ChargingProfileCapable, CreditCardPayable, Reservable, RfidReader, RemoteStartStopCapable, UnlockCapabale)
  }

  case class Evse(
    uid: String,
    last_updated: DateTime,
    status: ConnectorStatus,
    connectors: List[Connector],
    status_schedule: List[StatusSchedule] = Nil,
    capabilities: List[Capability] = Nil,
    evse_id: Option[String] = None,
    floor_level:	Option[String] = None,
    coordinates:	Option[GeoLocation] = None,
    physical_reference:	Option[String] = None,
    directions: List[DisplayText] = Nil,
    parking_restrictions:	List[ParkingRestriction] = Nil,
    images: List[Image] = Nil
    ){
    require(connectors.nonEmpty, "List of connector can't be empty!")
  }

  case class EvsePatch(
    uid: String,
    status: Option[ConnectorStatus] = None,
    connectors: Option[List[Connector]] = None,
    status_schedule: Option[List[StatusSchedule]] = None,
    capabilities: Option[List[Capability]] = None,
    evse_id: Option[String] = None,
    floor_level: Option[String] = None,
    coordinates: Option[GeoLocation] = None,
    physical_reference: Option[String] = None,
    directions: Option[List[DisplayText]] = None,
    parking_restrictions: Option[List[ParkingRestriction]] = None,
    images: Option[List[Image]] = None
  )

  val LatRegex = """-?[0-9]{1,2}\.[0-9]{1,6}"""
  val LonRegex = """-?[0-9]{1,3}\.[0-9]{1,6}"""

  case class AdditionalGeoLocation(
    latitude: String,
    longitude: String,
    name: Option[DisplayText] = None
  ){
    // we will suspend this hard validation until we have a way to continue parsing
    // the list even if individual locations cannot be deserialized
//    require(latitude.matches(LatRegex), s"latitude needs to conform to $LatRegex but was $latitude")
//    require(longitude.matches(LonRegex), s"longitude needs to conform to $LonRegex but was $longitude")
  }

  case class GeoLocation(
    latitude: String,
    longitude: String
    ){
    // we will suspend this hard validation until we have a way to continue parsing
    // the list even if individual locations cannot be deserialized
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
    val values = List(EvOnly, Plugged, Disabled, Customers, Motorcycles)
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
    val values = List(Available, Blocked, Charging, Inoperative, OutOfOrder, Planned, Removed, Reserved, Unknown)
  }

  case class StatusSchedule(
    period_begin: DateTime,
    period_end: Option[DateTime],
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
    val values = List(CHADEMO, `IEC_62196_T1`, `IEC_62196_T1_COMBO`, `IEC_62196_T2`,
      `IEC_62196_T2_COMBO`, `IEC_62196_T3A`, `IEC_62196_T3C`, `DOMESTIC_A`, `DOMESTIC_B`,
      `DOMESTIC_C`, `DOMESTIC_D`, `DOMESTIC_E`, `DOMESTIC_F`, `DOMESTIC_G`, `DOMESTIC_H`,
      `DOMESTIC_I`, `DOMESTIC_J`, `DOMESTIC_K`, `DOMESTIC_L`, `TESLA_R`, `TESLA_S`,
      `IEC_60309_2_single_16`, `IEC_60309_2_three_16`, `IEC_60309_2_three_32`, `IEC_60309_2_three_64`)
  }

  sealed trait ConnectorFormat extends Nameable
  object ConnectorFormat extends Enumerable[ConnectorFormat] {
    case object Socket extends ConnectorFormat {val name = "SOCKET"}
    case object Cable extends ConnectorFormat {val name = "CABLE"}
    val values = List(Socket, Cable)
  }

  sealed trait PowerType extends Nameable

  object PowerType extends Enumerable[PowerType] {
    case object AC1Phase extends PowerType {val name = "AC_1_PHASE"}
    case object AC3Phase extends PowerType {val name = "AC_3_PHASE"}
    case object DC extends PowerType {val name = "DC"}
    val values = List(AC1Phase, AC3Phase, DC)
  }

  case class LocationsResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now(),
    data: List[Location]
    ) extends SuccessResponse

  case class LocationResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now(),
    data: Location
  ) extends OcpiResponse[Option[String]]

  case class EvseResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now(),
    data: Evse
  ) extends OcpiResponse[Option[String]]

  case class ConnectorResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime = DateTime.now(),
    data: Connector
  ) extends OcpiResponse[Option[String]]
}
