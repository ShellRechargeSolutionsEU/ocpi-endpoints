package com.thenewmotion.ocpi.msgs.v2_1

import spray.json._

class SessionsSpec extends GenericSessionsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import SessionJsonProtocol._

  "Spray Json SessionsJsonProtocol" should {
    runTests()
  }
}
