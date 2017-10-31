package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCredentialsSpec
import io.circe.{Decoder, Encoder, Json}

class CredentialsSpecs extends GenericCredentialsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import CredentialsJsonProtocol._

  "Circe CredentialsJsonProtocol" should {
    runTests()
  }
}
