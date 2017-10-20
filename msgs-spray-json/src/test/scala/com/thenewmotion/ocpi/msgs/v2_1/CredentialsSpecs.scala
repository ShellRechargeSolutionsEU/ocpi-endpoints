package com.thenewmotion.ocpi.msgs
package v2_1

import spray.json._

class CredentialsSpecs extends GenericCredentialsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import DefaultJsonProtocol._
  import CredentialsJsonProtocol._

  "Spray CredentialsJsonProtocol" should {
    runTests()
  }
}
