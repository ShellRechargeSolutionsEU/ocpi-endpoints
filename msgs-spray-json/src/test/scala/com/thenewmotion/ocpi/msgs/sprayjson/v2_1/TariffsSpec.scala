package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericTariffsSpec
import spray.json._

class TariffsSpec extends GenericTariffsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import TariffsJsonProtocol._

  "Spray Json TariffsJsonProtocol" should {
    runTests()
  }

}
