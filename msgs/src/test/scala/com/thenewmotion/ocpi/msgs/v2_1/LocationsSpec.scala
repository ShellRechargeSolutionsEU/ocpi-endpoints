package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Locations.{Latitude, Longitude}
import org.specs2.mutable.Specification

class LocationsSpec extends Specification {

  "Latitude" should {
    "Validate range" >> {
      Latitude(34.546565) must not(throwA[Exception])
      Latitude(-92) must throwA[IllegalArgumentException]
      Latitude(92) must throwA[IllegalArgumentException]
    }

    "Trim decimal places to 6, when given a double, non strict mode" >> {
      Latitude(34.45775663434).value mustEqual 34.457757
      Latitude(34.45775663434).toString mustEqual "34.457757"
    }

    "Error when given a double with too much precision, strict mode" >> {
      Latitude.strict(34.45775663434) must throwA[IllegalArgumentException]
    }

    "Accept a double with correct precision, strict mode" >> {
      Latitude.strict(34.457756) must not(throwA[IllegalArgumentException])
    }

    "Parse a string" >> {
      Latitude("34.45775663434").value mustEqual 34.457757
      Latitude("abc") must throwA[IllegalArgumentException]
    }
  }

  "Longitude" should {
    "Validate range" >> {
      Longitude(34.546565) must not(throwA[Exception])
      Longitude(92) must not(throwA[IllegalArgumentException])
      Longitude(-181) must throwA[IllegalArgumentException]
      Longitude(181) must throwA[IllegalArgumentException]
    }

    "Trim decimal places to 6, when given a double, non strict mode" >> {
      Longitude(34.45775663434).value mustEqual 34.457757
      Longitude(34.45775663434).toString mustEqual "34.457757"
    }

    "Error when given a double with too much precision, strict mode" >> {
      Longitude.strict(34.45775663434) must throwA[IllegalArgumentException]
    }

    "Accept a double with correct precision, strict mode" >> {
      Longitude.strict(34.457756) must not(throwA[IllegalArgumentException])
    }

    "Parse a string" >> {
      Longitude("34.45775663434").value mustEqual 34.457757
      Longitude("abc") must throwA[IllegalArgumentException]
    }
  }
}
