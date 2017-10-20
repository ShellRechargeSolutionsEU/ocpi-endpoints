package com.thenewmotion.ocpi.msgs.v2_1

import spray.json._

class CommonTypesSpec extends GenericCommonTypesSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import DefaultJsonProtocol._

  "Spray DefaultJsonProtocol" should {
    runTests()
  }
}
