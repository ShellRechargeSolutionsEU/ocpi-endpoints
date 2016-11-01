package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.money._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes.GenericClientFailure
import org.joda.money.Money
import org.joda.time.DateTimeZone._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class CommonTypesSpecs extends SpecificationWithJUnit {

  import OcpiJsonProtocol._

  "DateTimeJsonFormat" should {
    val obj = new DateTime(2014, 1, 28, 17, 30, 0, UTC).withZone(DateTimeZone.getDefault)
    val str = "2014-01-28T17:30:00Z"
    "serialize" in {
      obj.toJson mustEqual JsString(str)
    }
    "extract" in {
      JsonParser("\"" + str + "\"").convertTo[DateTime].getMillis mustEqual obj.getMillis
    }
  }


  "MoneyJsonFormat" should {
    "deserialize" in {
      val money = 1 minorsOf EUR
      val jData =
        s"""
          |{
          | "currency": "EUR",
          | "amount": "0.000001"
          |}
        """.stripMargin.parseJson
      jData.convertTo[Money] mustEqual money
    }
  }

  "GenericResp" should {
    "deserialize" in new TestScope {
      genericErrorRespJson1.convertTo[ErrorResp] mustEqual genericErrorResp1
    }
    "serialize" in new TestScope {
      genericErrorResp1.toJson.toString mustEqual genericErrorRespJson1.compactPrint
    }
  }


  private trait TestScope extends Scope {

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val date1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

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
        GenericClientFailure.code,
        GenericClientFailure.defaultMessage,
        date1)
  }
}
