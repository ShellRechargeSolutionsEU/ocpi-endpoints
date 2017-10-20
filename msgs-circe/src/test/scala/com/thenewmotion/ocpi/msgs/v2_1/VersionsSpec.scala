package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class VersionsSpec extends GenericVersionsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import VersionsJsonProtocol._

  "Circe VersionsJsonProtocol" should {
    runTests()
  }
}
