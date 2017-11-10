package com.thenewmotion.ocpi.msgs
package circe.v2_1

import circe.SimpleStringEnumSerializer._
import v2_1.Locations._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._

trait LocationsJsonProtocol {

  def strict: Boolean

  implicit val connectorIdE: Encoder[ConnectorId] = stringEncoder(_.value)
  implicit val connectorIdD: Decoder[ConnectorId] = tryStringDecoder(ConnectorId.apply)

  implicit val locationIdE: Encoder[LocationId] = stringEncoder(_.value)
  implicit val locationIdD: Decoder[LocationId] = tryStringDecoder(LocationId.apply)

  implicit val evseUidE: Encoder[EvseUid] = stringEncoder(_.value)
  implicit val evseUidD: Decoder[EvseUid] = tryStringDecoder(EvseUid.apply)

  implicit val energySourceE: Encoder[EnergySource] = deriveEncoder
  implicit val energySourceD: Decoder[EnergySource] = deriveDecoder

  implicit val environmentalImpactE: Encoder[EnvironmentalImpact] = deriveEncoder
  implicit val environmentalImpactD: Decoder[EnvironmentalImpact] = deriveDecoder

  implicit val energyMixE: Encoder[EnergyMix] = deriveEncoder
  implicit val energyMixD: Decoder[EnergyMix] = deriveDecoder

  implicit val latitudeE: Encoder[Latitude] = stringEncoder(_.toString)
  implicit val latitudeD: Decoder[Latitude] =
    if (strict) tryStringDecoder(Latitude.strict) else tryStringDecoder(Latitude.apply)

  implicit val longitudeE: Encoder[Longitude] = stringEncoder(_.toString)
  implicit val longitudeD: Decoder[Longitude] =
    if (strict) tryStringDecoder(Longitude.strict) else tryStringDecoder(Longitude.apply)

  implicit val geoLocationE: Encoder[GeoLocation] = deriveEncoder
  implicit val geoLocationD: Decoder[GeoLocation] = deriveDecoder

  implicit val additionalGeoLocationE: Encoder[AdditionalGeoLocation] = deriveEncoder
  implicit val additionalGeoLocationD: Decoder[AdditionalGeoLocation] = deriveDecoder

  implicit val regularHoursE: Encoder[RegularHours] = deriveEncoder
  implicit val regularHoursD: Decoder[RegularHours] = deriveDecoder

  implicit val exceptionalPeriodE: Encoder[ExceptionalPeriod] = deriveEncoder
  implicit val exceptionalPeriodD: Decoder[ExceptionalPeriod] = deriveDecoder

  implicit val hoursE: Encoder[Hours] = deriveEncoder
  implicit val hoursD: Decoder[Hours] = deriveDecoder

  implicit val connectorE: Encoder[Connector] = deriveEncoder
  implicit val connectorD: Decoder[Connector] = deriveDecoder

  implicit val connectorPatchE: Encoder[ConnectorPatch] = deriveEncoder
  implicit val connectorPatchD: Decoder[ConnectorPatch] = deriveDecoder

  implicit val statusScheduleE: Encoder[StatusSchedule] = deriveEncoder
  implicit val statusScheduleD: Decoder[StatusSchedule] = deriveDecoder

  implicit val evseE: Encoder[Evse] = deriveEncoder
  implicit val evseD: Decoder[Evse] = deriveDecoder

  implicit val evsePatchE: Encoder[EvsePatch] = deriveEncoder
  implicit val evsePatchD: Decoder[EvsePatch] = deriveDecoder

  implicit val locationE: Encoder[Location] = deriveEncoder
  implicit val locationD: Decoder[Location] = deriveDecoder

  implicit val locationPatchE: Encoder[LocationPatch] = deriveEncoder
  implicit val locationPatchD: Decoder[LocationPatch] = deriveDecoder
}

object LocationsJsonProtocol extends LocationsJsonProtocol {
  override def strict = true
}
