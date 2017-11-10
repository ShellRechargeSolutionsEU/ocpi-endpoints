package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Tariffs._
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import com.thenewmotion.ocpi.msgs.sprayjson.SimpleStringEnumSerializer._
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

trait TariffsJsonProtocol {
  implicit val tariffIdFmt = new JsonFormat[TariffId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => TariffId(s)
      case _ => deserializationError("TariffId must be a string")
    }
    override def write(obj: TariffId) = JsString(obj.value)
  }

  implicit val priceComponentFormat = jsonFormat3(PriceComponent)

  implicit val tariffRestrictionsFormat = jsonFormat11(TariffRestrictions)

  implicit val tariffElementFormat = jsonFormat2(TariffElement)

  implicit val tariffFormat = jsonFormat7(Tariff)
}

object TariffsJsonProtocol extends TariffsJsonProtocol
