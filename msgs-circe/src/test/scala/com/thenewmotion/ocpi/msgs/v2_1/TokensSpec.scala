package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class TokensSpec extends GenericTokensSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import TokensJsonProtocol._

  "Circe TokenJsonProtocol" should {
    runTests()
  }
}
