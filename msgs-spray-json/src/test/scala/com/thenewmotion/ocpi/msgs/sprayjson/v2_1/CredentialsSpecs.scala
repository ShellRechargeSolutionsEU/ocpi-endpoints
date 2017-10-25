package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCredentialsSpec
import spray.json._

class CredentialsSpecs extends GenericCredentialsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import CredentialsJsonProtocol._

  "Spray CredentialsJsonProtocol" should {
    runTests()
  }
}
