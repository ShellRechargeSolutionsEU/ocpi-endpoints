package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericCommonTypesSpec
import io.circe.{Decoder, Encoder, Json}

class CommonTypesSpecs extends GenericCommonTypesSpec[Json, Decoder, Encoder] with CirceJsonSpec {
  import CommonJsonProtocol._

  "Circe DefaultJsonProtocol" should {
    runTests()
  }
}
