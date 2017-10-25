package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericVersionsSpec
import spray.json._

class VersionsSpec extends GenericVersionsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import DefaultJsonProtocol._
  import VersionsJsonProtocol._

  "Spray Json VersionsJsonProtocol" should {
    runTests()
  }
}
