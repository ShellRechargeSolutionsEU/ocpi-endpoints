package com.thenewmotion.ocpi.msgs.v2_1

import Cdrs._
import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import TariffsJsonProtocol._

trait CdrsJsonProtocol {
  private implicit val cdrIdFmt = new JsonFormat[CdrId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => CdrId(s)
      case _ => deserializationError("CdrId must be a string")
    }
    override def write(obj: CdrId) = JsString(obj.value)
  }

  private implicit val authMethodFormat =
    new SimpleStringEnumSerializer[AuthMethod](AuthMethod).enumFormat

  private implicit val cdrDimensionTypeFormat =
    new SimpleStringEnumSerializer[CdrDimensionType](CdrDimensionType).enumFormat

  private implicit val cdrDimensionFormat = jsonFormat2(CdrDimension)

  private implicit val chargingPeriodFormat = jsonFormat2(ChargingPeriod)

  implicit val cdrFormat = jsonFormat16(Cdr)
}

object CdrsJsonProtocol extends CdrsJsonProtocol