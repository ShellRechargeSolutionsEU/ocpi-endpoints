package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.OcpiDatetimeParser
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.chrono.ISOChronology
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class UtilsSpec extends Specification{

  sequential

  "OcpiDateTimeParser" should {

    val withMillisAndOffset = "2016-11-07T12:31:43.123+01:00"
    s"parse pattern with milliseconds and offset, e.g.: $withMillisAndOffset " in new Scope {
      OcpiDatetimeParser.toOcpiDateTime(withMillisAndOffset) must not beEmpty
    }

    val withTimezoneOffset = "2016-11-07T12:31:43+01:00"
    s"parse pattern with timezone offset, e.g.: $withTimezoneOffset " in new Scope {
      OcpiDatetimeParser.toOcpiDateTime(withTimezoneOffset) must not beEmpty
    }

    val withZliteral = "2016-11-07T11:31:43Z"
    s"parse pattern with 'Z' literal, e.g.: $withZliteral" in new Scope {
      OcpiDatetimeParser.toOcpiDateTime(withZliteral) must not beEmpty
    }

    val withoutOffsetOrZliteral = "2016-11-07T11:31:43"
    s"parse pattern without offset or 'Z' literal, e.g.: $withoutOffsetOrZliteral" in new Scope {
      val noTz = OcpiDatetimeParser.toOcpiDateTime(withoutOffsetOrZliteral)
      noTz.isDefined mustEqual true
      DateTimeZone.setDefault(DateTimeZone.UTC)
      noTz.get must_== new DateTime(2016, 11, 7, 11, 31, 43, 0, ISOChronology.getInstance)
    }
  }
}
