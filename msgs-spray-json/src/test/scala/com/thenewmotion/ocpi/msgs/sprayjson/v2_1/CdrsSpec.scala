package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCdrsSpec
import spray.json._

class CdrsSpec extends GenericCdrsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import CdrsJsonProtocol._

  "Spray Json CdrsJsonProtocol" should {
    runTests()
  }
}
