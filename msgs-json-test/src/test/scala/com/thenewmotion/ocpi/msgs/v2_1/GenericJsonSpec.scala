package com.thenewmotion.ocpi.msgs.v2_1

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.core.Fragments

import scala.language.higherKinds

trait GenericJsonSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends SpecificationWithJUnit {
  def parse(s: String): J

  def jsonStringToJson(s: String): J
  def jsonToObj[T : GenericJsonReader](j: J): T
  def objToJson[T : GenericJsonWriter](t: T): J

  def parseAs[T : GenericJsonReader](s: String): T = jsonToObj(parse(s))

  def testPair[T : GenericJsonWriter : GenericJsonReader](obj: T, json: J): Fragments = {
    "serialize" in {
      objToJson(obj) === json
    }
    "deserialize" in {
      jsonToObj(json) === obj
    }
  }
}
