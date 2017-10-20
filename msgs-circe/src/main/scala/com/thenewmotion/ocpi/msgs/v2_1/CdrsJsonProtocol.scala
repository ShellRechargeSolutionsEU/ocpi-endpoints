package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._
import TariffsJsonProtocol._
import CommonJsonProtocol._
import LocationsJsonProtocol._

trait CdrsJsonProtocol {
  implicit val CcdrIdE: Encoder[CdrId] = stringEncoder(_.value)
  implicit val cdrIdD: Decoder[CdrId] = tryStringDecoder(CdrId.apply)

  private implicit val authMethodE: Encoder[AuthMethod] =
    SimpleStringEnumSerializer.encoder(AuthMethod)

  private implicit val authMethodD: Decoder[AuthMethod] =
    SimpleStringEnumSerializer.decoder(AuthMethod)

  private implicit val cdrDimensionTypeE: Encoder[CdrDimensionType] =
    SimpleStringEnumSerializer.encoder(CdrDimensionType)

  private implicit val cdrDimensionTypeD: Decoder[CdrDimensionType] =
    SimpleStringEnumSerializer.decoder(CdrDimensionType)

  implicit val cdrDimensionE: Encoder[CdrDimension] = deriveEncoder
  implicit val cdrDimensionD: Decoder[CdrDimension] = deriveDecoder

  implicit val chargingPeriodE: Encoder[ChargingPeriod] = deriveEncoder
  implicit val chargingPeriodD: Decoder[ChargingPeriod] = deriveDecoder

  implicit val cdrE: Encoder[Cdr] = deriveEncoder
  implicit val cdrD: Decoder[Cdr] = deriveDecoder
}

object CdrsJsonProtocol extends CdrsJsonProtocol
