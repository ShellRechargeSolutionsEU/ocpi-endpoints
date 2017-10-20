package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._
import CommonJsonProtocol._

trait LocationsJsonProtocol {

  implicit val connectorIdE: Encoder[ConnectorId] = stringEncoder(_.value)
  implicit val connectorIdD: Decoder[ConnectorId] = tryStringDecoder(ConnectorId.apply)

  implicit val locationIdE: Encoder[LocationId] = stringEncoder(_.value)
  implicit val locationIdD: Decoder[LocationId] = tryStringDecoder(LocationId.apply)

  implicit val evseUidE: Encoder[EvseUid] = stringEncoder(_.value)
  implicit val evseUidD: Decoder[EvseUid] = tryStringDecoder(EvseUid.apply)

  private implicit val capabilityE: Encoder[Capability] = SimpleStringEnumSerializer.encoder(Capability)
  private implicit val capabilityD: Decoder[Capability] = SimpleStringEnumSerializer.decoder(Capability)

  private implicit val connectorStatusE: Encoder[ConnectorStatus] = SimpleStringEnumSerializer.encoder(ConnectorStatus)
  private implicit val connectorStatusD: Decoder[ConnectorStatus] = SimpleStringEnumSerializer.decoder(ConnectorStatus)

  private implicit val connectorTypeE: Encoder[ConnectorType] = SimpleStringEnumSerializer.encoder(ConnectorType)
  private implicit val connectorTypeD: Decoder[ConnectorType] = SimpleStringEnumSerializer.decoder(ConnectorType)

  private implicit val connectorFormatE: Encoder[ConnectorFormat] = SimpleStringEnumSerializer.encoder(ConnectorFormat)
  private implicit val connectorFormatD: Decoder[ConnectorFormat] = SimpleStringEnumSerializer.decoder(ConnectorFormat)

  private implicit val powerTypeE: Encoder[PowerType] = SimpleStringEnumSerializer.encoder(PowerType)
  private implicit val powerTypeD: Decoder[PowerType] = SimpleStringEnumSerializer.decoder(PowerType)

  private implicit val locationTypeE: Encoder[LocationType] = SimpleStringEnumSerializer.encoder(LocationType)
  private implicit val locationTypeD: Decoder[LocationType] = SimpleStringEnumSerializer.decoder(LocationType)

  private implicit val parkingRestrictionE: Encoder[ParkingRestriction] =
    SimpleStringEnumSerializer.encoder(ParkingRestriction)
  private implicit val parkingRestrictionD: Decoder[ParkingRestriction] =
    SimpleStringEnumSerializer.decoder(ParkingRestriction)

  private implicit val facilityE: Encoder[Facility] = SimpleStringEnumSerializer.encoder(Facility)
  private implicit val facilityD: Decoder[Facility] = SimpleStringEnumSerializer.decoder(Facility)

  private implicit val energySourceCategoryE: Encoder[EnergySourceCategory] =
    SimpleStringEnumSerializer.encoder(EnergySourceCategory)
  private implicit val energySourceCategoryD: Decoder[EnergySourceCategory] =
    SimpleStringEnumSerializer.decoder(EnergySourceCategory)

  private implicit val environmentalImpactCategoryE: Encoder[EnvironmentalImpactCategory] =
    SimpleStringEnumSerializer.encoder(EnvironmentalImpactCategory)
  private implicit val environmentalImpactCategoryD: Decoder[EnvironmentalImpactCategory] =
    SimpleStringEnumSerializer.decoder(EnvironmentalImpactCategory)

  private[v2_1] implicit val energySourceE: Encoder[EnergySource] = deriveEncoder
  private[v2_1] implicit val energySourceD: Decoder[EnergySource] = deriveDecoder

  private[v2_1] implicit val environmentalImpactE: Encoder[EnvironmentalImpact] = deriveEncoder
  private[v2_1] implicit val environmentalImpactD: Decoder[EnvironmentalImpact] = deriveDecoder

  private[v2_1] implicit val energyMixE: Encoder[EnergyMix] = deriveEncoder
  private[v2_1] implicit val energyMixD: Decoder[EnergyMix] = deriveDecoder

  private implicit val geoLocationE: Encoder[GeoLocation] = deriveEncoder
  private implicit val geoLocationD: Decoder[GeoLocation] = deriveDecoder

  private implicit val additionalGeoLocationE: Encoder[AdditionalGeoLocation] = deriveEncoder
  private implicit val additionalGeoLocationD: Decoder[AdditionalGeoLocation] = deriveDecoder

  private implicit val regularHoursE: Encoder[RegularHours] = deriveEncoder
  private implicit val regularHoursD: Decoder[RegularHours] = deriveDecoder

  private[v2_1] implicit val exceptionalPeriodE: Encoder[ExceptionalPeriod] = deriveEncoder
  private[v2_1] implicit val exceptionalPeriodD: Decoder[ExceptionalPeriod] = deriveDecoder

  private[v2_1] implicit val hoursE: Encoder[Hours] = deriveEncoder
  private[v2_1] implicit val hoursD: Decoder[Hours] = deriveDecoder

  implicit val connectorE: Encoder[Connector] = deriveEncoder
  implicit val connectorD: Decoder[Connector] = deriveDecoder

  implicit val connectorPatchE: Encoder[ConnectorPatch] = deriveEncoder
  implicit val connectorPatchD: Decoder[ConnectorPatch] = deriveDecoder

  private implicit val statusScheduleE: Encoder[StatusSchedule] = deriveEncoder
  private implicit val statusScheduleD: Decoder[StatusSchedule] = deriveDecoder

  implicit val evseE: Encoder[Evse] = deriveEncoder
  implicit val evseD: Decoder[Evse] = deriveDecoder

  implicit val evsePatchE: Encoder[EvsePatch] = deriveEncoder
  implicit val evsePatchD: Decoder[EvsePatch] = deriveDecoder

  implicit val locationE: Encoder[Location] = deriveEncoder
  implicit val locationD: Decoder[Location] = deriveDecoder

  implicit val locationPatchE: Encoder[LocationPatch] = deriveEncoder
  implicit val locationPatchD: Decoder[LocationPatch] = deriveDecoder
}

object LocationsJsonProtocol extends LocationsJsonProtocol
