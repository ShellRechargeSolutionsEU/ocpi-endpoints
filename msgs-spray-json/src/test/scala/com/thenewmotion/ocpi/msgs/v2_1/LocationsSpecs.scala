package com.thenewmotion.ocpi.msgs.v2_1

import spray.json._

class LocationsSpecs extends GenericLocationsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import LocationsJsonProtocol._

  "Spray Json LocationsJsonProtocol" should {
    runTests()
  }
}
