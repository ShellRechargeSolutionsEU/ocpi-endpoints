package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCdrsSpec
import io.circe.{Decoder, Encoder, Json}

class CdrsSpec extends GenericCdrsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import CdrsJsonProtocol._

  "Circe Json CdrsJsonProtocol" should {
    runTests()
  }
}
