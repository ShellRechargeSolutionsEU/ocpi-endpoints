package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericVersionsSpec
import io.circe.{Decoder, Encoder, Json}

class VersionsSpec extends GenericVersionsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import VersionsJsonProtocol._

  "Circe VersionsJsonProtocol" should {
    runTests()
  }
}
