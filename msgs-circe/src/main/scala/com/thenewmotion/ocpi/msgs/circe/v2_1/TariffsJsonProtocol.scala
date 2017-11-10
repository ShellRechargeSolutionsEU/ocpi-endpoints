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

  implicit val priceComponentE: Encoder[PriceComponent] = deriveEncoder
  implicit val priceComponentD: Decoder[PriceComponent] = deriveDecoder

  implicit val tariffRestrictionsE: Encoder[TariffRestrictions] = deriveEncoder
  implicit val tariffRestrictionsD: Decoder[TariffRestrictions] = deriveDecoder

  implicit val tariffElementE: Encoder[TariffElement] = deriveEncoder
  implicit val tariffElementD: Decoder[TariffElement] = deriveDecoder

  implicit val tariffE: Encoder[Tariff] = deriveEncoder
  implicit val tariffD: Decoder[Tariff] = deriveDecoder
}

object TariffsJsonProtocol extends TariffsJsonProtocol
