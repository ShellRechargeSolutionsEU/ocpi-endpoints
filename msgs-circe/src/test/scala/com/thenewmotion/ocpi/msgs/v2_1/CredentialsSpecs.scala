package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class CredentialsSpecs extends GenericCredentialsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import CredentialsJsonProtocol._

  "Circe CredentialsJsonProtocol" should {
    runTests()
  }
}
