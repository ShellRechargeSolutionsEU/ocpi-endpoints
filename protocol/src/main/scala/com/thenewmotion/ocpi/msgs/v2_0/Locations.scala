package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.{Enumerable, Nameable}
import com.thenewmotion.time.Imports._
import com.thenewmotion.money.CurrencyUnit
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes._


object Locations {

  case class Location(
    id: String,
    `type`:	LocationType,
    name:	Option[String],
    address: String,
    city:	String,
    postal_code: String,
    country:	String,
    coordinates:	GeoLocation,
    evses: Option[List[Evse]],
    directions:	Option[String] = None,
    operator: Option[BusinessDetails] = None,
    opening_times: Option[List[Hours]] = None,
    charging_when_closed: Option[Boolean] = None,
    images: Option[List[Image]] = None

    )

  sealed trait LocationType extends Nameable
  object LocationTypeEnum extends Enumerable[LocationType] {
    case object OnStreet extends LocationType {val name = "on_street"}
    case object ParkingGarage extends LocationType {val name = "parking_garage"}
    case object UndergroundGarage extends LocationType {val name = "underground_garage"}
    case object ParkingLot extends LocationType {val name = "parking_lot"}
    case object Other extends LocationType {val name = "other"}
    case object Unknown extends LocationType {val name = "unknown"}
    val values = List(OnStreet, ParkingGarage, UndergroundGarage,
      ParkingLot, Other, Unknown)
  }
  case class Hours()


  case class Operator(
    identifier: Option[String], // unique identifier of the operator
    phone: Option[String],
    url: Option[String]
    )

  case class Power(
    current: Option[CurrentType],
    amperage: Int,
    voltage: Int
    )

  sealed trait PricingUnit extends Nameable

  object PricingUnitEnum extends Enumerable[PricingUnit] {
    case object Session extends PricingUnit {val name = "session"}
    case object KWhToEV extends PricingUnit {val name = "kwhtoev"}
    case object OccupancyHours extends PricingUnit {val name = "occupancyhours"}
    case object ChargingHours extends PricingUnit {val name = "charginghours"}
    case object IdleHours extends PricingUnit {val name = "idlehours"}
    val values = List(Session, KWhToEV, OccupancyHours, ChargingHours, IdleHours)
  }

  sealed trait PeriodType extends Nameable

  object PeriodTypeEnum extends Enumerable[PeriodType] {
    case object Charging extends PeriodType {val name = "Charging"}
    case object Parking extends PeriodType {val name = "Parking"}
    val values = List(Charging, Parking)
  }

  // not in OCPP 2.0
//
//  case class Validity(
//    period_type: Option[PeriodType],
//    time: Option[String]
//    )

  case class Tariff(
    tariff_id: String,
    price_taxed: Option[Double],
    price_untaxed: Option[Double],
    pricing_unit: PricingUnit,
    tax_pct: Option[Double],
    currency: CurrencyUnit,
    //    validity_rule: Option[Validity],  // not in OCPP 2.0
    condition: Option[String],
    display_text: DisplayText
    )

  case class  PriceScheme(
    price_scheme_id: Int,
    start_date: Option[DateTime],
    expiry_date: Option[DateTime],
    tariff: Option[List[Tariff]],
    display_text: DisplayText
    )

  case class Connector(
    id: String,
    standard: ConnectorType,
    format: ConnectorFormat,
    price_schemes: Option[List[PriceScheme]],
    power_type:	CurrentType,
    voltage: Int,
    amperage: Int,
    terms_and_conditions: Option[Url] = None
    )



  sealed trait Capability extends Nameable

  //Shouldn't that be free text field? Different capability different tariff_type per
  // charging profile. It needs to be agreed between the parties
  //will do it like this for Enexis and discuss it on the OCPI meeting next month
  object CapabilityEnum extends Enumerable[Capability]{
    case object ChargingProfileCapable extends Capability {val name = "charging_profile_cap"}
    case object Reservable extends Capability {val name = "reservable"}
    val values = List(ChargingProfileCapable, Reservable)
  }

  case class Evse(
    id: String,
    location_id: String,
    status: ConnectorStatus,
    capabilities: Option[List[String]],
    connectors: List[Connector],
    floor_level:	Option[String] = None,
    coordinates:	Option[GeoLocation] = None,
    physical_number:	Option[Int] = None,
    directions: Option[String] = None,
    parking_restrictions:	Option[ParkingRestriction] = None,
    images: Option[Image] = None
    )

  case class GeoLocation(
    latitude: String,
    longitude: String
    )

  case class ParkingRestriction()
  case class Image()

  sealed trait ConnectorStatus extends Nameable

  object ConnectorStatusEnum extends Enumerable[ConnectorStatus] {
    case object Available extends ConnectorStatus {val name = "AVAILABLE"}
    //case object Occupied extends ConnectorStatus {val name = "occupied"} // not in current OCPI
    case object Reserved extends ConnectorStatus {val name = "RESERVED"}
    case object Charging extends ConnectorStatus {val name = "CHARGING"}
    case object Blocked extends ConnectorStatus {val name = "BLOCKED"}
    case object OutOfService extends ConnectorStatus {val name = "OUTOFORDER"}
    case object Inoperative extends ConnectorStatus {val name = "INOPERATIVE"}
    //case object Unknown extends ConnectorStatus {val name = "unknown"}
    val values = List(Available, Reserved, Charging, Blocked, OutOfService, Inoperative)
  }

  sealed trait ConnectorType extends Nameable

  object ConnectorTypeEnum extends Enumerable[ConnectorType] {
    case object	Chademo	extends ConnectorType {val name = "Chademo"}
    case object	`IEC-62196-T1`	extends ConnectorType {val name = "IEC-62196-T1"}
    case object	`IEC-62196-T1-COMBO`	extends ConnectorType {val name = "IEC-62196-T1-COMBO"}
    case object	`IEC-62196-T2`	extends ConnectorType {val name = "IEC-62196-T2"}
    case object	`IEC-62196-T2-COMBO`	extends ConnectorType {val name = "IEC-62196-T2-COMBO"}
    case object	`IEC-62196-T3A`	extends ConnectorType {val name = "IEC-62196-T3A"}
    case object	`IEC-62196-T3C`	extends ConnectorType {val name = "IEC-62196-T3C"}
    case object	`DOMESTIC-A`	extends ConnectorType {val name = "DOMESTIC-A"}
    case object	`DOMESTIC-B`	extends ConnectorType {val name = "DOMESTIC-B"}
    case object	`DOMESTIC-C`	extends ConnectorType {val name = "DOMESTIC-C"}
    case object	`DOMESTIC-D`	extends ConnectorType {val name = "DOMESTIC-D"}
    case object	`DOMESTIC-E`	extends ConnectorType {val name = "DOMESTIC-E"}
    case object	`DOMESTIC-F`	extends ConnectorType {val name = "DOMESTIC-F"}
    case object	`DOMESTIC-G`	extends ConnectorType {val name = "DOMESTIC-G"}
    case object	`DOMESTIC-H`	extends ConnectorType {val name = "DOMESTIC-H"}
    case object	`DOMESTIC-I`	extends ConnectorType {val name = "DOMESTIC-I"}
    case object	`DOMESTIC-J`	extends ConnectorType {val name = "DOMESTIC-J"}
    case object	`DOMESTIC-K`	extends ConnectorType {val name = "DOMESTIC-K"}
    case object	`DOMESTIC-L`	extends ConnectorType {val name = "DOMESTIC-L"}
    case object	`TESLA-R`	extends ConnectorType {val name = "TESLA-R"}
    case object	`TESLA-S`	extends ConnectorType {val name = "TESLA-S"}
    case object	`IEC-60309-2-single-16`	extends ConnectorType {val name = "IEC-60309-2-single-16"}
    case object	`IEC-60309-2-three-16`	extends ConnectorType {val name = "IEC-60309-2-three-16"}
    case object	`IEC-60309-2-three-32`	extends ConnectorType {val name = "IEC-60309-2-three-32"}
    case object	`IEC-60309-2-three-64`	extends ConnectorType {val name = "IEC-60309-2-three-64"}
    val values = List(Chademo, `IEC-62196-T1`, `IEC-62196-T1-COMBO`, `IEC-62196-T2`,
      `IEC-62196-T2-COMBO`, `IEC-62196-T3A`, `IEC-62196-T3C`, `DOMESTIC-A`, `DOMESTIC-B`,
      `DOMESTIC-C`, `DOMESTIC-D`, `DOMESTIC-E`, `DOMESTIC-F`, `DOMESTIC-G`, `DOMESTIC-H`,
      `DOMESTIC-I`, `DOMESTIC-J`, `DOMESTIC-K`, `DOMESTIC-L`, `TESLA-R`, `TESLA-S`,
      `IEC-60309-2-single-16`, `IEC-60309-2-three-16`, `IEC-60309-2-three-32`, `IEC-60309-2-three-64`)
  }

  sealed trait ConnectorFormat extends Nameable
  object ConnectorFormatEnum extends Enumerable[ConnectorFormat] {
    case object Socket extends ConnectorFormat {val name = "SOCKET"}
    case object Cable extends ConnectorFormat {val name = "CABLE"}
    val values = List(Socket, Cable)
  }

  sealed trait CurrentType extends Nameable

  object CurrentTypeEnum extends Enumerable[CurrentType] {
    case object AC1Phase extends CurrentType {val name = "AC_1_PHASE"}
    case object AC3Phases extends CurrentType {val name = "AC_3_PHASE"}
    case object DC extends CurrentType {val name = "DC"}
    val values = List(AC1Phase, AC3Phases, DC)
  }


  case class LocationsData(
    locations: List[Location]
    )
  case class LocationResp(
    status_code: Int,
    status_message: Option[String] = None,
    timestamp: DateTime,
    data: LocationsData
    ) extends OcpiResponse

}
