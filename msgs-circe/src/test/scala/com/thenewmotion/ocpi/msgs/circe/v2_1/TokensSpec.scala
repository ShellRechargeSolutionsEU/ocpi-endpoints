package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericTokensSpec
import io.circe.{Decoder, Encoder, Json}

class TokensSpec extends GenericTokensSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import TokensJsonProtocol._

  "Circe TokenJsonProtocol" should {
    runTests()
  }
}
