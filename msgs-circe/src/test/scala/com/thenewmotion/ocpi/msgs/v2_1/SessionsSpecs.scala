package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class SessionsSpecs extends GenericSessionsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import SessionJsonProtocol._

  "Circe SessionsJsonProtocol" should {
    runTests()
  }
}
