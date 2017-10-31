package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import java.time.{LocalTime, ZonedDateTime}

import v2_1.Locations._
import DefaultJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{BusinessDetails, DisplayText, Image}
import sprayjson.SimpleStringEnumSerializer
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

trait LocationsJsonProtocol {

  private def deserializeLocation(
    id: LocationId,
    lastUpdated: ZonedDateTime,
    `type`: LocationType,
    name: Option[String],
    address: String,
    city: String,
    postalCode: String,
    country: CountryCode,
    coordinates: GeoLocation,
    relatedLocations: Option[Iterable[AdditionalGeoLocation]],
    evses: Option[Iterable[Evse]],
    directions: Option[Iterable[DisplayText]],
    operator: Option[BusinessDetails],
    suboperator: Option[BusinessDetails],
    owner: Option[BusinessDetails],
    facilities: Option[Iterable[Facility]],
    timeZone: Option[String],
    openingTimes: Option[Hours],
    chargingWhenClosed: Option[Boolean],
    images: Option[Iterable[Image]],
    energyMix: Option[EnergyMix]
  ) =
    Location(
      id,
      lastUpdated,
      `type`,
      name,
      address,
      city,
      postalCode,
      country,
      coordinates,
      relatedLocations.getOrElse(Nil),
      evses.getOrElse(Nil),
      directions.getOrElse(Nil),
      operator,
      suboperator,
      owner,
      facilities.getOrElse(Nil),
      timeZone,
      openingTimes,
      chargingWhenClosed,
      images.getOrElse(Nil),
      energyMix
    )

  private def deserializeEnergyMix(
    isGreenEnergy: Boolean,
    energySources: Option[Iterable[EnergySource]],
    environImpact: Option[Iterable[EnvironmentalImpact]],
    supplierName: Option[String],
    energyProductName: Option[String]
  ) =
    EnergyMix(
      isGreenEnergy,
      energySources.getOrElse(Nil),
      environImpact.getOrElse(Nil),
      supplierName,
      energyProductName
    )

  private def deserializeHours(
    twentyfourseven: Option[Boolean],
    regularHours: Option[Iterable[RegularHours]],
    exceptionalOpenings: Option[Iterable[ExceptionalPeriod]],
    exceptionalClosings: Option[Iterable[ExceptionalPeriod]]
  ) = Hours(
    twentyfourseven.fold(false)(_ == true),
    regularHours.getOrElse(Nil),
    exceptionalOpenings.getOrElse(Nil),
    exceptionalClosings.getOrElse(Nil)
  )

  private def deserializeEvse(
    uid: EvseUid,
    lastUpdated: ZonedDateTime,
    status: ConnectorStatus,
    connectors: Option[Iterable[Connector]],
    statusSchedule: Option[Iterable[StatusSchedule]],
    capabilities: Option[Iterable[Capability]],
    evseId: Option[String],
    floorLevel: Option[String],
    coordinates: Option[GeoLocation],
    physicalReference: Option[String],
    directions: Option[Iterable[DisplayText]],
    parkingRestrictions: Option[Iterable[ParkingRestriction]],
    images: Option[Iterable[Image]]
  ) = Evse(
    uid,
    lastUpdated,
    status,
    connectors.getOrElse(Nil),
    statusSchedule.getOrElse(Nil),
    capabilities.getOrElse(Nil),
    evseId,
    floorLevel,
    coordinates,
    physicalReference,
    directions.getOrElse(Nil),
    parkingRestrictions.getOrElse(Nil),
    images.getOrElse(Nil)
  )

  private implicit val capabilityFormat =
    new SimpleStringEnumSerializer[Capability](Capability).enumFormat

  private implicit val connectorStatusFormat =
    new SimpleStringEnumSerializer[ConnectorStatus](ConnectorStatus).enumFormat

  private implicit val connectorTypeFormat =
    new SimpleStringEnumSerializer[ConnectorType](ConnectorType).enumFormat

  private implicit val connectorFormatFormat =
    new SimpleStringEnumSerializer[ConnectorFormat](ConnectorFormat).enumFormat

  private implicit val currentTypeFormat =
    new SimpleStringEnumSerializer[PowerType](PowerType).enumFormat

  private implicit val locationTypeFormat =
    new SimpleStringEnumSerializer[LocationType](LocationType).enumFormat

  private implicit val parkingRestrictionTypeFormat =
    new SimpleStringEnumSerializer[ParkingRestriction](ParkingRestriction).enumFormat

  private implicit val facilityTypeFormat =
    new SimpleStringEnumSerializer[Facility](Facility).enumFormat

  private implicit val energySourceCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnergySourceCategory](EnergySourceCategory).enumFormat

  private implicit val environmentalImpactCategoryTypeFormat =
    new SimpleStringEnumSerializer[EnvironmentalImpactCategory](EnvironmentalImpactCategory).enumFormat

  private implicit val energySourceFormat = jsonFormat2(EnergySource)
  private implicit val environmentalImpactFormat = jsonFormat2(EnvironmentalImpact)

  implicit val energyMixFormat = new JsonFormat[EnergyMix] {
    val readFormat = jsonFormat5(deserializeEnergyMix)
    val writeFormat = jsonFormat5(EnergyMix.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: EnergyMix): JsValue = writeFormat.write(obj)
  }

  private implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  private implicit val additionalGeoLocationFormat = jsonFormat3(AdditionalGeoLocation)
  private implicit val regularHoursFormat = jsonFormat3(RegularHours.apply(_: Int, _: LocalTime, _: LocalTime))
  private implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  private[v2_1] implicit val hoursFormat = new JsonFormat[Hours] {
    val readFormat = jsonFormat4(deserializeHours)
    val writeFormat = jsonFormat4(Hours.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Hours): JsValue = writeFormat.write(obj)
  }

  implicit val connectorIdFmt = new JsonFormat[ConnectorId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => ConnectorId(s)
      case _           => deserializationError("ConnectorId must be a string")
    }
    override def write(obj: ConnectorId) = JsString(obj.value)
  }

  implicit val locationIdFmt = new JsonFormat[LocationId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => LocationId(s)
      case _           => deserializationError("LocationId must be a string")
    }
    override def write(obj: LocationId) = JsString(obj.value)
  }

  implicit val evseUidFmt = new JsonFormat[EvseUid] {
    override def read(json: JsValue) = json match {
      case JsString(s) => EvseUid(s)
      case _           => deserializationError("EvseUid must be a string")
    }
    override def write(obj: EvseUid) = JsString(obj.value)
  }

  implicit val connectorFormat = jsonFormat9(Connector)
  implicit val connectorPatchFormat = jsonFormat9(ConnectorPatch)
  private implicit val statusScheduleFormat = jsonFormat3(StatusSchedule)
  implicit val evseFormat = new RootJsonFormat[Evse] {
    val readFormat = jsonFormat13(deserializeEvse)
    val writeFormat = jsonFormat13(Evse.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Evse): JsValue = writeFormat.write(obj)
  }
  implicit val evsePatchFormat = jsonFormat13(EvsePatch)

  implicit val locationFormat = new RootJsonFormat[Location] {
    val readFormat = jsonFormat21(deserializeLocation)
    val writeFormat = jsonFormat21(Location.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Location): JsValue = writeFormat.write(obj)
  }

  implicit val locationPatchFormat = jsonFormat21(LocationPatch.apply)
}

object LocationsJsonProtocol extends LocationsJsonProtocol
