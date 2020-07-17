package com.thenewmotion.ocpi.msgs
package circe.v2_1

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

  implicit val energySourceE: Encoder[EnergySource] = deriveConfiguredEncoder
  implicit val energySourceD: Decoder[EnergySource] = deriveConfiguredDecoder

  implicit val environmentalImpactE: Encoder[EnvironmentalImpact] = deriveConfiguredEncoder
  implicit val environmentalImpactD: Decoder[EnvironmentalImpact] = deriveConfiguredDecoder

  implicit val energyMixE: Encoder[EnergyMix] = deriveConfiguredEncoder
  implicit val energyMixD: Decoder[EnergyMix] = deriveConfiguredDecoder

  implicit val latitudeE: Encoder[Latitude] = stringEncoder(_.toString)
  implicit val latitudeD: Decoder[Latitude] =
    if (strict) tryStringDecoder(Latitude.strict) else tryStringDecoder(Latitude.apply)

  implicit val longitudeE: Encoder[Longitude] = stringEncoder(_.toString)
  implicit val longitudeD: Decoder[Longitude] =
    if (strict) tryStringDecoder(Longitude.strict) else tryStringDecoder(Longitude.apply)

  implicit val geoLocationE: Encoder[GeoLocation] = deriveConfiguredEncoder
  implicit val geoLocationD: Decoder[GeoLocation] = deriveConfiguredDecoder

  implicit val additionalGeoLocationE: Encoder[AdditionalGeoLocation] = deriveConfiguredEncoder
  implicit val additionalGeoLocationD: Decoder[AdditionalGeoLocation] = deriveConfiguredDecoder

  implicit val regularHoursE: Encoder[RegularHours] = deriveConfiguredEncoder
  implicit val regularHoursD: Decoder[RegularHours] = deriveConfiguredDecoder

  implicit val exceptionalPeriodE: Encoder[ExceptionalPeriod] = deriveConfiguredEncoder
  implicit val exceptionalPeriodD: Decoder[ExceptionalPeriod] = deriveConfiguredDecoder

  implicit val hoursE: Encoder[Hours] = deriveConfiguredEncoder
  implicit val hoursD: Decoder[Hours] = deriveConfiguredDecoder

  implicit val connectorE: Encoder[Connector] = deriveConfiguredEncoder
  implicit val connectorD: Decoder[Connector] = deriveConfiguredDecoder

  implicit val connectorPatchE: Encoder[ConnectorPatch] = deriveConfiguredEncoder
  implicit val connectorPatchD: Decoder[ConnectorPatch] = deriveConfiguredDecoder

  implicit val statusScheduleE: Encoder[StatusSchedule] = deriveConfiguredEncoder
  implicit val statusScheduleD: Decoder[StatusSchedule] = deriveConfiguredDecoder

  implicit val evseE: Encoder[Evse] = deriveConfiguredEncoder
  implicit val evseD: Decoder[Evse] = deriveConfiguredDecoder

  implicit val evsePatchE: Encoder[EvsePatch] = deriveConfiguredEncoder
  implicit val evsePatchD: Decoder[EvsePatch] = deriveConfiguredDecoder

  implicit val locationE: Encoder[Location] = deriveConfiguredEncoder
  implicit val locationD: Decoder[Location] = deriveConfiguredDecoder

  implicit val locationPatchE: Encoder[LocationPatch] = deriveConfiguredEncoder
  implicit val locationPatchD: Decoder[LocationPatch] = deriveConfiguredDecoder
}

object LocationsJsonProtocol extends LocationsJsonProtocol {
  override def strict = true
}
