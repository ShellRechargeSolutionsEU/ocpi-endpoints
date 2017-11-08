package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import spray.json.{JsString, JsValue, JsonFormat, deserializationError}
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import TariffsJsonProtocol._
import sprayjson.SimpleStringEnumSerializer
import v2_1.Cdrs._

trait CdrsJsonProtocol {
  implicit val cdrIdFmt = new JsonFormat[CdrId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => CdrId(s)
      case _ => deserializationError("CdrId must be a string")
    }
    override def write(obj: CdrId) = JsString(obj.value)
  }

  implicit val authMethodFormat =
    new SimpleStringEnumSerializer[AuthMethod](AuthMethod).enumFormat

  implicit val cdrDimensionTypeFormat =
    new SimpleStringEnumSerializer[CdrDimensionType](CdrDimensionType).enumFormat

  implicit val cdrDimensionFormat = jsonFormat2(CdrDimension)

  implicit val chargingPeriodFormat = jsonFormat2(ChargingPeriod)

  implicit val cdrFormat = jsonFormat16(Cdr)
}

object CdrsJsonProtocol extends CdrsJsonProtocol
