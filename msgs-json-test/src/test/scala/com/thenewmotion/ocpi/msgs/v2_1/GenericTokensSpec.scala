package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Tokens.LocationReferences
import org.specs2.specification.Scope
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericTokensSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit locRefD: GenericJsonReader[LocationReferences]
  ): Fragments = {
    "LocationReferences" should {
      "deserialize missing fields of cardinality '*' to empty lists" in new Scope {
        val locRefs: LocationReferences =
          parseAs[LocationReferences]("""
            | {
            |   "location_id": "loc1"
            | }
          """.stripMargin)

        locRefs.evseUids mustEqual Nil
        locRefs.connectorIds mustEqual Nil
      }
    }
  }

}
