package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCommonTypesSpec
import spray.json._

class CommonTypesSpec extends GenericCommonTypesSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import DefaultJsonProtocol._

  "Spray DefaultJsonProtocol" should {
    runTests(successPagedRes)
  }
}
