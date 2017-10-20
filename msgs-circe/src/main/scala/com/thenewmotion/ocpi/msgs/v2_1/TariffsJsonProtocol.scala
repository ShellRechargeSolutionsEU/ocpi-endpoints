package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_1.Tariffs._
import io.circe.{Decoder, Encoder}
import io.circe.generic.extras.semiauto._
import CommonJsonProtocol._
import LocationsJsonProtocol._

class TariffsJsonProtocol {

  implicit val tariffIdE: Encoder[TariffId] = stringEncoder(_.value)
  implicit val tariffIdD: Decoder[TariffId] = tryStringDecoder(TariffId.apply)

  private implicit val tariffDimensionTypeE: Encoder[TariffDimensionType] =
    SimpleStringEnumSerializer.encoder(TariffDimensionType)

  private implicit val tariffDimensionTypeD: Decoder[TariffDimensionType] =
    SimpleStringEnumSerializer.decoder(TariffDimensionType)

  private implicit val dayOfWeekE: Encoder[DayOfWeek] =
    SimpleStringEnumSerializer.encoder(DayOfWeek)

  private implicit val dayOfWeekD: Decoder[DayOfWeek] =
    SimpleStringEnumSerializer.decoder(DayOfWeek)

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
