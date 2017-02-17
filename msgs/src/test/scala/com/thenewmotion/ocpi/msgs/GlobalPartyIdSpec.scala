package com.thenewmotion.ocpi.msgs

import org.specs2.mutable.Specification

class GlobalPartyIdSpec extends Specification {

  "GlobalPartyId" should {
    "Accept a valid country code and party id combo, uppercasing if nescessary" >> {
      val res = GlobalPartyId("nl", "tnm")
      res.countryCode mustEqual "NL"
      res.partyId mustEqual "TNM"
    }

    "Accept a single string containing country code and party id combo, uppercasing if nescessary" >> {
      val res = GlobalPartyId("nltnm")
      res.countryCode mustEqual "NL"
      res.partyId mustEqual "TNM"
    }

    "throw a wobbly when given" >> {
      "an invalid country code" >> {
        GlobalPartyId("zz", "tnm") must throwA[IllegalArgumentException]
      }

      "a too long country code" >> {
        GlobalPartyId("NLD", "tnm") must throwA[IllegalArgumentException]
      }

      "a too short country code" >> {
        GlobalPartyId("N", "tnm") must throwA[IllegalArgumentException]
      }

      "a country code with invalid chars" >> {
        GlobalPartyId("++", "tnm") must throwA[IllegalArgumentException]
      }

      "a too short party id" >> {
        GlobalPartyId("nl", "tn") must throwA[IllegalArgumentException]
      }

      "a too long party id" >> {
        GlobalPartyId("nl", "tnnm") must throwA[IllegalArgumentException]
      }

      "a party id with invalid chars" >> {
        GlobalPartyId("nl", "+-$") must throwA[IllegalArgumentException]
      }

      "a single string of invalid length" >> {
        GlobalPartyId("a") must throwA[IllegalArgumentException]
      }
    }
  }

}
