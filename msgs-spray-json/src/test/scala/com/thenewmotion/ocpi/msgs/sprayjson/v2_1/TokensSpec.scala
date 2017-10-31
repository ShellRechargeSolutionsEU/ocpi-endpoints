package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericTokensSpec
import spray.json._

class TokensSpec extends GenericTokensSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec  {

  import TokensJsonProtocol._

  "Spray Json TokenJsonProtocol" should {
    runTests()
  }
}
