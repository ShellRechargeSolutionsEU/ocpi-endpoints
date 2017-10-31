package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.GenericTariffsSpec
import io.circe.{Decoder, Encoder, Json}

class TariffsSpec extends GenericTariffsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import TariffsJsonProtocol._

  "Circe TariffsJsonProtocol" should {
    runTests()
  }

}
