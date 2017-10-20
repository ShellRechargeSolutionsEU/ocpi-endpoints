package com.thenewmotion.ocpi.msgs.v2_1

import spray.json._

class TokensSpec extends GenericTokensSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec  {

  import TokensJsonProtocol._

  "Spray Json TokenJsonProtocol" should {
    runTests()
  }
}
