package com.thenewmotion.ocpi
package msgs
package v2_1

import spray.json._

class VersionsSpec extends GenericVersionsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import DefaultJsonProtocol._
  import VersionsJsonProtocol._

  "Spray Json VersionsJsonProtocol" should {
    runTests()
  }
}
