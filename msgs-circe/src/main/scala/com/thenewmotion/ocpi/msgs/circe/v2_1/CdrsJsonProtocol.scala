package com.thenewmotion.ocpi.msgs
package circe.v2_1

import v2_1.Cdrs._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import TariffsJsonProtocol._
import CommonJsonProtocol._
import LocationsJsonProtocol._

trait CdrsJsonProtocol {
  implicit val cdrIdE: Encoder[CdrId] = stringEncoder(_.value)
  implicit val cdrIdD: Decoder[CdrId] = tryStringDecoder(CdrId.apply)

  implicit val cdrDimensionE: Encoder[CdrDimension] = deriveConfiguredEncoder
  implicit val cdrDimensionD: Decoder[CdrDimension] = deriveConfiguredDecoder

  implicit val chargingPeriodE: Encoder[ChargingPeriod] = deriveConfiguredEncoder
  implicit val chargingPeriodD: Decoder[ChargingPeriod] = deriveConfiguredDecoder

  implicit val cdrE: Encoder[Cdr] = deriveConfiguredEncoder
  implicit val cdrD: Decoder[Cdr] = deriveConfiguredDecoder
}

object CdrsJsonProtocol extends CdrsJsonProtocol
