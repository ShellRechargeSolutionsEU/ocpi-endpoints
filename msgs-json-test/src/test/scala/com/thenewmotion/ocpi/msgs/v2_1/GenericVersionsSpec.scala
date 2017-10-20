package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.ZonedDateTimeParser
import com.thenewmotion.ocpi.msgs.Url
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber._
import com.thenewmotion.ocpi.msgs.Versions._
import org.specs2.specification.core.Fragments
import scala.language.higherKinds

trait GenericVersionsSpec[J, GenericJsonReader[_], GenericJsonWriter[_]] extends
  GenericJsonSpec[J, GenericJsonReader, GenericJsonWriter] {

  def runTests()(
    implicit versionR: GenericJsonReader[List[Version]],
    versionW: GenericJsonWriter[List[Version]],
    versionDetailsR: GenericJsonReader[VersionDetails],
    versionDetailsW: GenericJsonWriter[VersionDetails]
  ): Fragments = {

    "VersionsResp" should {
      testPair(versionResp, parse(versionRespJson1))
    }

    "VersionDetailsResp" should {
      testPair(version21Details, parse(version21DetailsRespJson))
    }
  }

  val date1 = ZonedDateTimeParser.parse("2010-01-01T00:00:00Z")

  val version20 = Version(
    `2.0`, Url("https://example.com/ocpi/cpo/2.0/")
  )
  val version21 = Version(
    `2.1`, Url("https://example.com/ocpi/cpo/2.1/")
  )

  val versionResp = List(version20, version21)

  val credentialsEndpoint = Endpoint(
    EndpointIdentifier.Credentials,
    Url("https://example.com/ocpi/cpo/2.0/credentials/"))

  val locationsEndpoint = Endpoint(
    EndpointIdentifier.Locations,
    Url("https://example.com/ocpi/cpo/2.0/locations/"))

  val version21Details = VersionDetails(
    version = `2.1`,
    endpoints = List(credentialsEndpoint, locationsEndpoint)
  )

  val versionRespJson1 =
    s"""
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
   """.stripMargin

  val version21DetailsRespJson =
    s"""
       |  {
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
   """.stripMargin

  lazy val version20DetailsIncompleteRespJson =
    s"""
       |  {
       |    "version": "2.0",
       |    "endpoints": [
       |        {
       |            "identifier": "locations",
       |            "url": "https://example.com/ocpi/cpo/2.0/locations/"
       |        }
       |    ]
       |  }
   """.stripMargin

}
