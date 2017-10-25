package com.thenewmotion.ocpi.msgs.sprayjson.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericLocationsSpec
import spray.json._

class LocationsSpecs extends GenericLocationsSpec[JsValue, JsonReader, JsonWriter] with SprayJsonSpec {

  import LocationsJsonProtocol._

  "Spray Json LocationsJsonProtocol" should {
    runTests()
  }
}
