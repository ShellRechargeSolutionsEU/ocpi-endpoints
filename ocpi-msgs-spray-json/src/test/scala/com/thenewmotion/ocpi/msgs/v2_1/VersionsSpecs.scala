package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber._
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import spray.json._

class VersionsSpecs extends SpecificationWithJUnit {

  import OcpiJsonProtocol._

  "VersionsResp" should {
    "deserialize" in new VersionsTestScope {
      versionRespJson1.convertTo[SuccessWithDataResp[List[Version]]] mustEqual versionResp
    }
    "serialize" in new VersionsTestScope {
      versionResp.toJson.toString mustEqual versionRespJson1.compactPrint
    }
  }


  "VersionDetailsResp" should {
    "succeed to deserialize if minimum expected endpoints included" in new VersionsTestScope {
      version21DetailsRespJson.convertTo[SuccessWithDataResp[VersionDetails]] mustEqual version21DetailsResp
    }
    "serialize" in new VersionsTestScope {
      version21DetailsResp.toJson.toString mustEqual version21DetailsRespJson.compactPrint
    }
    "fail to deserialize if missing some expected endpoints" in new VersionsTestScope {
      version20DetailsIncompleteRespJson.convertTo[SuccessWithDataResp[VersionDetails]] must throwA[IllegalArgumentException]
    }
  }

  private trait VersionsTestScope extends Scope {

    import OcpiStatusCode._

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val date1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

    val version20 = Version(
      `2.0`, "https://example.com/ocpi/cpo/2.0/"
    )
    val version21 = Version(
      `2.1`, "https://example.com/ocpi/cpo/2.1/"
    )

    val versionResp = SuccessWithDataResp(GenericSuccess, Some("Success"),
      date1, List(version20, version21))

    val credentialsEndpoint = Endpoint(
      EndpointIdentifier.Credentials,
      "https://example.com/ocpi/cpo/2.0/credentials/")

    val locationsEndpoint = Endpoint(
      EndpointIdentifier.Locations,
      "https://example.com/ocpi/cpo/2.0/locations/")

    val version21Details = VersionDetails(
      version = `2.1`,
      endpoints = List(credentialsEndpoint, locationsEndpoint)
    )

    val version21DetailsResp = SuccessWithDataResp(
      GenericSuccess, Some("Success"),
      date1, version21Details
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
         |        "version": "2.0",
         |        "url": "https://example.com/ocpi/cpo/2.0/"
         |    },
         |    {
         |        "version": "2.1",
         |        "url": "https://example.com/ocpi/cpo/2.1/"
         |    }
         |  ]
         | }
     """.stripMargin.parseJson


    val version21DetailsRespJson =
      s"""
         |{
         |  "status_code": 1000,
         |  "status_message": "Success",
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data":{
         |    "version": "2.1",
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


    lazy val version20DetailsIncompleteRespJson =
      s"""
         |{
         |  "status_code": 1000,
         |  "status_message": "Success",
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data":{
         |    "version": "2.0",
         |    "endpoints": [
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
