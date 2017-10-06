package com.thenewmotion.ocpi.msgs.v2_1

import java.time.{LocalDate, LocalTime, ZoneOffset, ZonedDateTime}
import com.thenewmotion.ocpi.msgs.{CurrencyCode, ErrorResp, SuccessResp}
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.{GenericClientFailure, GenericSuccess}
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

  "ErrorResp" should {
    "deserialize" in new TestScope {
      genericErrorRespJson1.convertTo[ErrorResp] mustEqual genericErrorResp1
    }
    "serialize" in new TestScope {
      genericErrorResp1.toJson mustEqual genericErrorRespJson1
    }
  }

  "SuccessResp" should {
    "deserialize response without data" in new TestScope {
      successRespJson1.convertTo[SuccessResp[Unit]] mustEqual successResp1
    }
    "serialize response without data" in new TestScope {
      successResp1.toJson mustEqual successRespJson1
    }
    "deserialize response with data" in new TestScope {
      successRespJson2.convertTo[SuccessResp[Int]] mustEqual successResp2
    }
    "serialize response with data" in new TestScope {
      successResp2.toJson mustEqual successRespJson2
    }
  }

  "CurrencyCode" should {
    "deserialize" in new TestScope {
      JsString("EUR").convertTo[CurrencyCode] mustEqual CurrencyCode("EUR")
    }
    "serialize" in new TestScope {
      CurrencyCode("GBP").toJson mustEqual JsString("GBP")
    }
  }

  "Time" should {
    "deserialize" in new TestScope {
      JsString("13:25").convertTo[LocalTime] mustEqual LocalTime.of(13, 25)
    }
    "serialize" in new TestScope {
      LocalTime.of(13, 25).toJson mustEqual JsString("13:25")
    }
  }

  "Date" should {
    "deserialize" in new TestScope {
      JsString("2017-09-13").convertTo[LocalDate] mustEqual LocalDate.of(2017,9, 13)
    }
    "serialize" in new TestScope {
      LocalDate.of(2017,9, 13).toJson mustEqual JsString("2017-09-13")
    }
  }


  private trait TestScope extends Scope {

    val date1 = OcpiDateTimeParser.parse("2010-01-01T00:00:00Z")

    val successRespJson1 =
      s"""
         |{
         |  "status_code" : 1000,
         |  "status_message" : "It worked!",
         |  "timestamp": "2010-01-01T00:00:00Z"
         |}
     """.stripMargin.parseJson

    val successResp1 =
      SuccessResp(
        GenericSuccess,
        Some("It worked!"),
        date1)

    val successRespJson2 =
      s"""
         |{
         |  "status_code" : 1000,
         |  "status_message" : "It worked!",
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data": 42
         |}
     """.stripMargin.parseJson

    val successResp2 =
      SuccessResp(
        GenericSuccess,
        Some("It worked!"),
        date1, 42)

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
