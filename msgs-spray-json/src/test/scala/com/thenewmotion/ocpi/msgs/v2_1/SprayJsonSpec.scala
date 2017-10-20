package com.thenewmotion.ocpi.msgs.v2_1

import spray.json.{JsValue, _}

trait SprayJsonSpec extends GenericJsonSpec[JsValue, JsonReader, JsonWriter] {

  override def parse(s: String): JsValue = s.parseJson

  override def jsonStringToJson(s: String): JsValue = JsString(s)

  override def jsonToObj[T : JsonReader](j: JsValue): T = j.convertTo[T]

  override def objToJson[T : JsonWriter](t: T): JsValue = t.toJson
}
