package com.thenewmotion.ocpi.msgs
package circe.v2_1

import v2_1.Tariffs._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import LocationsJsonProtocol._

trait TariffsJsonProtocol {

  implicit val tariffIdE: Encoder[TariffId] = stringEncoder(_.value)
  implicit val tariffIdD: Decoder[TariffId] = tryStringDecoder(TariffId.apply)

  implicit val priceComponentE: Encoder[PriceComponent] = deriveConfiguredEncoder
  implicit val priceComponentD: Decoder[PriceComponent] = deriveConfiguredDecoder

  implicit val tariffRestrictionsE: Encoder[TariffRestrictions] = deriveConfiguredEncoder
  implicit val tariffRestrictionsD: Decoder[TariffRestrictions] = deriveConfiguredDecoder

  implicit val tariffElementE: Encoder[TariffElement] = deriveConfiguredEncoder
  implicit val tariffElementD: Decoder[TariffElement] = deriveConfiguredDecoder

  implicit val tariffE: Encoder[Tariff] = deriveConfiguredEncoder
  implicit val tariffD: Decoder[Tariff] = deriveConfiguredDecoder
}

object TariffsJsonProtocol extends TariffsJsonProtocol
