package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class LocationsSpecs extends GenericLocationsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import LocationsJsonProtocol._

  "Circe LocationsJsonProtocol" should {
    runTests()
  }
}
