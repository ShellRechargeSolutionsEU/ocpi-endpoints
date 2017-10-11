package com.thenewmotion.ocpi.msgs.v2_1

import java.time.LocalTime
import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import Locations._
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}
import DefaultJsonProtocol._

trait LocationsJsonProtocol {
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
    val readFormat = jsonFormat5(EnergyMix.deserialize)
    val writeFormat = jsonFormat5(EnergyMix.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: EnergyMix): JsValue = writeFormat.write(obj)
  }

  private implicit val geoLocationFormat = jsonFormat2(GeoLocation)
  private implicit val additionalGeoLocationFormat = jsonFormat3(AdditionalGeoLocation)
  private implicit val regularHoursFormat = jsonFormat3(RegularHours.apply(_: Int, _: LocalTime, _: LocalTime))
  private implicit val exceptionalPeriodFormat = jsonFormat2(ExceptionalPeriod)
  private[v2_1] implicit val hoursFormat = new JsonFormat[Hours] {
    val readFormat = jsonFormat4(Hours.deserialize)
    val writeFormat = jsonFormat4(Hours.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Hours): JsValue = writeFormat.write(obj)
  }

  implicit val connectorIdFmt = new JsonFormat[ConnectorId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => ConnectorId(s)
      case _ => deserializationError("ConnectorId must be a string")
    }
    override def write(obj: ConnectorId) = JsString(obj.value)
  }

  implicit val locationIdFmt = new JsonFormat[LocationId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => LocationId(s)
      case _ => deserializationError("LocationId must be a string")
    }
    override def write(obj: LocationId) = JsString(obj.value)
  }

  implicit val evseUidFmt = new JsonFormat[EvseUid] {
    override def read(json: JsValue) = json match {
      case JsString(s) => EvseUid(s)
      case _ => deserializationError("EvseUid must be a string")
    }
    override def write(obj: EvseUid) = JsString(obj.value)
  }

  implicit val connectorFormat = jsonFormat9(Connector)
  implicit val connectorPatchFormat = jsonFormat8(ConnectorPatch)
  private implicit val statusScheduleFormat = jsonFormat3(StatusSchedule)
  implicit val evseFormat = new RootJsonFormat[Evse] {
    val readFormat = jsonFormat13(Evse.deserialize)
    val writeFormat = jsonFormat13(Evse.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Evse): JsValue = writeFormat.write(obj)
  }
  implicit val evsePatchFormat = jsonFormat12(EvsePatch)

  implicit val locationFormat = new RootJsonFormat[Location] {
    val readFormat = jsonFormat21(Location.deserialize)
    val writeFormat = jsonFormat21(Location.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Location): JsValue = writeFormat.write(obj)
  }

  implicit val locationPatchFormat = jsonFormat21(LocationPatch)
}

object LocationsJsonProtocol extends LocationsJsonProtocol
