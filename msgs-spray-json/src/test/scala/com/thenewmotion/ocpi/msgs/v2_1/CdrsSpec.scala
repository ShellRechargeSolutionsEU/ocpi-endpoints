package com.thenewmotion.ocpi.msgs.v2_1

import spray.json._

class CdrsSpec extends GenericCdrsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import CdrsJsonProtocol._

  "Spray Json CdrsJsonProtocol" should {
    runTests()
  }
}
