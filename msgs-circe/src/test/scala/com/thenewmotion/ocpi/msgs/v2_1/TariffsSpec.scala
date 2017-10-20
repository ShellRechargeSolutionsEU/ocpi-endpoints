package com.thenewmotion.ocpi.msgs.v2_1

import io.circe.{Decoder, Encoder, Json}

class TariffsSpec extends GenericTariffsSpec[Json, Decoder, Encoder] with CirceJsonSpec {

  import TariffsJsonProtocol._

  "Circe TariffsJsonProtocol" should {
    runTests()
  }

}
