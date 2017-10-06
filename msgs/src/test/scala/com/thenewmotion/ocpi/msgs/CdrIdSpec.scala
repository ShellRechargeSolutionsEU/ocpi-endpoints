package com.thenewmotion.ocpi.msgs

import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.CdrId
import org.specs2.mutable.Specification

class CdrIdSpec extends Specification {
  "CdrId" should {
    "be case insensitive" >> {
      CdrId("ABC") mustEqual CdrId("abC")
    }

    "be 36 chars or less" >> {
      CdrId("1234567890123456789012345678901234567890") must throwA[IllegalArgumentException]
    }
  }
}
