package com.thenewmotion.ocpi
package handshake

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.Specs2RouteTest
import HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ImageCategory.Operator
import msgs.v2_1.CommonTypes.{CountryCode, GlobalPartyId, Image, PartyId, BusinessDetails => OcpiBusinessDetails}
import msgs.v2_1.Credentials.{Creds, OurToken, TheirToken}
import msgs.v2_1.OcpiStatusCode.GenericSuccess
import msgs.Versions.VersionNumber._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future
import scalaz._

class HandshakeRouteSpec(implicit ee: ExecutionEnv) extends Specification with Specs2RouteTest with Mockito {

  "credentials endpoint" should {
    "accept the credentials they sent us to connect to them" in new CredentialsTestScope {
      val theirLog = credsToConnectToThem.businessDetails.logo.get
      val theirCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.businessDetails.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.businessDetails.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.partyId}",
           |    "country_code": "${credsToConnectToThem.countryCode}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = `application/json`, string = theirCredsData)

      Post("/credentials", body) ~> credentialsRoute.route(selectedVersion, theirGlobalId) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token.value)
      }
    }

    "return the credentials we have set for them to connect to us" in new CredentialsTestScope {
      handshakeService.credsToConnectToUs(any) returns \/-(credsToConnectToUs)

      Get("/credentials") ~> credentialsRoute.route(selectedVersion, theirGlobalId) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token.value)
      }
    }

    "return error if no credentials to connect to us stored for that token" in new CredentialsTestScope {
      Get("/credentials") ~>
      credentialsRoute.route(selectedVersion, theirGlobalId) ~>
      check {
        status === BadRequest
      }
    }

    "accept the update of the credentials they sent us to connect to them" in new CredentialsTestScope {
      handshakeService.credsToConnectToUs(any) returns \/-(credsToConnectToUs)
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any, any) returns
        Future.successful(\/-(newCredsToConnectToUs))

      val theirLog = credsToConnectToThem.businessDetails.logo.get
      val theirNewCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.businessDetails.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.businessDetails.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.partyId}",
           |    "country_code": "${credsToConnectToThem.countryCode}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = `application/json`, string = theirNewCredsData)

      Put("/credentials", body) ~> credentialsRoute.route(selectedVersion, theirGlobalId) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(newCredsToConnectToUs.token.value)
      }
    }
    "reject indicating the reason if trying to update credentials for a token we are still waiting for its registration request" in new CredentialsTestScope {
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any, any) returns
        Future.successful(-\/(WaitingForRegistrationRequest))


      val theirLog = credsToConnectToThem.businessDetails.logo.get
      val theirNewCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.businessDetails.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.businessDetails.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.partyId}",
           |    "country_code": "${credsToConnectToThem.countryCode}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = `application/json`, string = theirNewCredsData)

      Put("/credentials", body) ~>
      credentialsRoute.route(selectedVersion, theirGlobalId) ~>
      check {
        status === BadRequest
      }
    }
  }

  trait CredentialsTestScope extends Scope {

    val theirPartyId = PartyId("EOP")
    val theirCountryCode = CountryCode("NL")
    val theirGlobalId = GlobalPartyId(theirCountryCode, theirPartyId)

    // their details
    val credsToConnectToThem = Creds(
      token = OurToken("ebf3b399-779f-4497-9b9d-ac6ad3cc44d2"),
      url = "https://them.com/ocpi/cpo/versions",
      businessDetails = OcpiBusinessDetails(
        "Example Operator",
        Some(Image("http://them.com/images/logo.png", Operator, "png")),
        Some("http://them.com")),
      partyId = theirPartyId,
      countryCode = theirCountryCode)

    // our details
    val ourVersionsUrl = "https://us.com/ocpi/msp/versions"
    val selectedVersion = `2.0`
    val tokenToConnectToUs = TheirToken("aaa3b399-779f-4497-9b9d-ac6ad3cc44aa")

    val partyId = PartyId("TNM")
    val countryCode = CountryCode("NL")

    val credsToConnectToUs = Creds(
      token = tokenToConnectToUs,
      url = ourVersionsUrl,
      businessDetails = OcpiBusinessDetails("Us", None, Some("http://us.com")),
      partyId = partyId,
      countryCode = countryCode)
    val newCredsToConnectToUs = credsToConnectToUs.copy()

    // mock
    val handshakeService = mock[HandshakeService]

    //default mocks
    handshakeService.reactToHandshakeRequest(any, any, any)(any, any) returns
      Future.successful(\/-(credsToConnectToUs))
    handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
      theirGlobalId, credsToConnectToThem.token, credsToConnectToThem.url) returns
      Future.successful(\/-(credsToConnectToThem))
    handshakeService.credsToConnectToUs(any) returns -\/(UnknownPartyToken)

    val credentialsRoute = new HandshakeRoute(handshakeService)
  }
}
