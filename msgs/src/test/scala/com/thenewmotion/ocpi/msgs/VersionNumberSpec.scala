package com.thenewmotion.ocpi.msgs

import com.thenewmotion.ocpi.msgs.Versions.VersionNumber
import org.specs2.mutable.Specification

class VersionNumberSpec extends Specification {

  "VersionNumber" should {
    "decode regular version string" >> {
      VersionNumber("2.1") mustEqual VersionNumber(2, 1)
    }

    "decode patch version string" >> {
      VersionNumber("2.1.1") mustEqual VersionNumber(2, 1, Some(1))
    }

    "toString without patch" >> {
      VersionNumber(2, 1).toString mustEqual "2.1"
    }

    "toString with patch" >> {
      VersionNumber(2, 1, 1).toString mustEqual "2.1.1"
    }

    "throw an error given an invalid version string" >> {
      VersionNumber("a.b") must throwA[IllegalArgumentException]
    }

    "be correctly comparable" >> {
      VersionNumber("2.1") > VersionNumber("2.0")
      VersionNumber("2.1") == VersionNumber("2.1")
      VersionNumber("2.1") == VersionNumber("2.1.0")
      VersionNumber("2.1.1") > VersionNumber("2.1")
      VersionNumber("2.1.2") > VersionNumber("2.1.1")
      VersionNumber("2.2.1") > VersionNumber("2.1.1")
    }
  }
}
