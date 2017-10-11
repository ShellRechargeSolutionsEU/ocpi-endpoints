package com.thenewmotion.ocpi.msgs.v2_1

import Tariffs._
import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import DefaultJsonProtocol._
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}
import LocationsJsonProtocol._

class TariffsJsonProtocol {
  private implicit val tariffIdFmt = new JsonFormat[TariffId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => TariffId(s)
      case _ => deserializationError("TariffId must be a string")
    }
    override def write(obj: TariffId) = JsString(obj.value)
  }

  private implicit val tariffDimensionTypeFormat =
    new SimpleStringEnumSerializer[TariffDimensionType](TariffDimensionType).enumFormat

  private implicit val dayOfWeekFormat =
    new SimpleStringEnumSerializer[DayOfWeek](DayOfWeek).enumFormat

  private implicit val priceComponentFormat = jsonFormat3(PriceComponent)

  implicit val tariffRestrictionsFormat = jsonFormat11(TariffRestrictions)

  private implicit val tariffElementFormat = jsonFormat2(TariffElement)

  implicit val tariffFormat = jsonFormat7(Tariff)
}

object TariffsJsonProtocol extends TariffsJsonProtocol