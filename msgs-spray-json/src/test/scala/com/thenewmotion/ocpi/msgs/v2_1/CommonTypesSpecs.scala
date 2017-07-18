package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{ZoneOffset, ZonedDateTime}

import com.thenewmotion.ocpi.msgs.ErrorResp
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericClientFailure
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._
import com.thenewmotion.ocpi.OcpiDateTimeParser

class CommonTypesSpecs extends SpecificationWithJUnit {

  import OcpiJsonProtocol._

  "DateTimeJsonFormat" should {
    val obj = ZonedDateTime.of(2014, 1, 28, 17, 30, 0, 0, ZoneOffset.UTC)
    val str = "2014-01-28T17:30:00Z"
    "serialize" in {
      obj.toJson mustEqual JsString(str)
    }
    "extract" in {
      JsonParser("\"" + str + "\"").convertTo[ZonedDateTime].toInstant.toEpochMilli mustEqual obj.toInstant.toEpochMilli
    }
  }

  "GenericResp" should {
    "deserialize" in new TestScope {
      genericErrorRespJson1.convertTo[ErrorResp] mustEqual genericErrorResp1
    }
    "serialize" in new TestScope {
      genericErrorResp1.toJson mustEqual genericErrorRespJson1
    }
  }

  private trait TestScope extends Scope {

    val date1 = OcpiDateTimeParser.parse("2010-01-01T00:00:00Z")

    val genericErrorRespJson1 =
    s"""
       |{
       |  "status_code" : 2000,
       |  "status_message" : "Client error",
       |  "timestamp": "2010-01-01T00:00:00Z"
       |}
     """.stripMargin.parseJson

    val genericErrorResp1 =
      ErrorResp(
        GenericClientFailure,
        Some("Client error"),
        date1)
  }
}
