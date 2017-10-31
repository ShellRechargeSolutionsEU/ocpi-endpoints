package com.thenewmotion.ocpi

import java.time.{ZoneOffset, ZonedDateTime}

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class OcpiDateTimeParserSpec extends Specification{

  sequential

  "OcpiDateTimeParser" should {

    "format a given datetime correctly" in new Scope {
      ZonedDateTimeParser.format(
        ZonedDateTime.of(2016, 11, 7, 11, 31, 43, 0, ZoneOffset.UTC)) mustEqual "2016-11-07T11:31:43Z"
    }

    val withZliteral = "2016-11-07T11:31:43Z"
    s"parse pattern with 'Z' literal, e.g.: $withZliteral" in new Scope {
      ZonedDateTimeParser.parseOpt(withZliteral) must not beEmpty
    }

    val withoutOffsetOrZliteral = "2016-11-07T11:31:43"
    s"parse pattern without offset or 'Z' literal, e.g.: $withoutOffsetOrZliteral" in new Scope {
      val noTz = ZonedDateTimeParser.parseOpt(withoutOffsetOrZliteral)
      noTz.isDefined mustEqual true
      noTz.get must_== ZonedDateTime.of(2016, 11, 7, 11, 31, 43, 0, ZoneOffset.UTC)
    }

    val withMillisAndOffset = "2016-11-07T12:31:43+01:00"
    s"fail to parse pattern with offset other than UTC, e.g.: $withMillisAndOffset " in new Scope {
      ZonedDateTimeParser.parse(withMillisAndOffset) must throwA[IllegalArgumentException]
    }
  }
}
