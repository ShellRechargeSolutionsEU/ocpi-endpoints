package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericSessionsSpec
import io.circe.{Decoder, Encoder, Json}

class SessionsSpecs extends GenericSessionsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import SessionJsonProtocol._

  "Circe SessionsJsonProtocol" should {
    runTests()
  }
}
