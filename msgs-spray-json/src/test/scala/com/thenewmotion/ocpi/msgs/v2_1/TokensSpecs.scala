package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Tokens.LocationReferences
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class TokensSpecs extends SpecificationWithJUnit {

  import TokensJsonProtocol._

  "LocationReferences" should {
    "deserialize missing fields of cardinality '*' to empty lists" in new Scope {
      val locRefs =
        """
          | {
          |   "location_id": "loc1"
          | }
        """.stripMargin.parseJson.convertTo[LocationReferences]

      locRefs.evseUids mustEqual Nil
      locRefs.connectorIds mustEqual Nil
    }
  }

}
