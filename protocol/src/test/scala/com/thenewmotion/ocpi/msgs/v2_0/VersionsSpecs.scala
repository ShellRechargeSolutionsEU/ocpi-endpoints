package com.thenewmotion.ocpi.msgs.v2_0

import com.thenewmotion.ocpi.msgs.v2_0.Versions.EndpointIdentifier$
import com.thenewmotion.ocpi.msgs.v2_0.Versions.{VersionDetailsResp, VersionsResp}
import OcpiJsonProtocol._
import Versions._
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class VersionsSpecs extends SpecificationWithJUnit {


  "VersionsResp" should {
    "deserialize" in new VersionsTestScope {
      versionRespJson1.convertTo[VersionsResp] mustEqual versionResp
    }
    "serialize" in new VersionsTestScope {
      versionResp.toJson.toString mustEqual versionRespJson1.compactPrint
    }
  }


  "VersionDetailsResp" should {
    "deserialize" in new VersionsTestScope {
      version20DetailsRespJson1.convertTo[VersionDetailsResp] mustEqual version20DetailsResp
    }
    "serialize" in new VersionsTestScope {
      version20DetailsResp.toJson.toString mustEqual version20DetailsRespJson1.compactPrint
    }
  }

  private trait VersionsTestScope extends Scope {

    import OcpiStatusCodes._

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val date1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

    val version19 = Version(
      "1.9", "https://example.com/ocpi/cpo/1.9/"
    )
    val version20 = Version(
      "2.0", "https://example.com/ocpi/cpo/2.0/"
    )

    val versionResp = VersionsResp(GenericSuccess.code, Some(GenericSuccess.default_message),
      date1, List(version19, version20))

    val credentialsEndpoint = Endpoint(
      EndpointIdentifier.Credentials,
      "https://example.com/ocpi/cpo/2.0/credentials/")

    val locationsEndpoint = Endpoint(
      EndpointIdentifier.Locations,
      "https://example.com/ocpi/cpo/2.0/locations/")

    val version20Details = VersionDetails(
      version = "2.0",
      endpoints = List(credentialsEndpoint, locationsEndpoint)
    )

    val version20DetailsResp = VersionDetailsResp(
      GenericSuccess.code, Some(GenericSuccess.default_message),
      date1, version20Details
    )

    val versionRespJson1 =
      s"""
         |{
         |  "status_code": 1000,
         |  "status_message": "Success",
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data":
         |  [
         |    {
         |        "version": "1.9",
         |        "url": "https://example.com/ocpi/cpo/1.9/"
         |    },
         |    {
         |        "version": "2.0",
         |        "url": "https://example.com/ocpi/cpo/2.0/"
         |    }
         |  ]
         | }
     """.stripMargin.parseJson


    val version20DetailsRespJson1 =
      s"""
         |{
         |  "status_code": 1000,
         |  "status_message": "Success",
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data":{
         |    "version": "2.0",
         |    "endpoints": [
         |        {
         |            "identifier": "credentials",
         |            "url": "https://example.com/ocpi/cpo/2.0/credentials/"
         |        },
         |        {
         |            "identifier": "locations",
         |            "url": "https://example.com/ocpi/cpo/2.0/locations/"
         |        }
         |    ]
         |  }
         |}
     """.stripMargin.parseJson



  }
}
