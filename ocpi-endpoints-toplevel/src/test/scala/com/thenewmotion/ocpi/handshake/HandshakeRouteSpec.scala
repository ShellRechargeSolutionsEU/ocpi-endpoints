package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.handshake.HandshakeError.UnknownPartyToken
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{Image, ImageCategory, BusinessDetails => OcpiBusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber._
import com.thenewmotion.spray.testkit.Specs2RouteTest
import org.joda.time.DateTime
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCharsets, HttpEntity}
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

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirCredsData)

      Post("/credentials", body) ~> credentialsRoute.route(selectedVersion, tokenToConnectToUs) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token)
      }
    }

    "return the credentials we have set for them to connect to us" in new CredentialsTestScope {
      handshakeService.credsToConnectToUs(any) returns \/-(credsToConnectToUs)

      Get("/credentials") ~> credentialsRoute.route(selectedVersion, tokenToConnectToUs) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token)
      }
    }

    "return error if no credentials to connect to us stored for that token" in new CredentialsTestScope {
      Get("/credentials") ~>
      credentialsRoute.route(selectedVersion, tokenToConnectToUs) ~>
      check {
        status === BadRequest
      }
    }

    "accept the update of the credentials they sent us to connect to them" in new CredentialsTestScope {
      handshakeService.credsToConnectToUs(any) returns \/-(credsToConnectToUs)
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any) returns
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

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirNewCredsData)

      Put("/credentials", body) ~> credentialsRoute.route(selectedVersion, tokenToConnectToUs) ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(newCredsToConnectToUs.token)
      }
    }
    "reject indicating the reason if trying to update credentials for a token we are still waiting for its registration request" in new CredentialsTestScope {
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any) returns
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

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirNewCredsData)

      Put("/credentials", body) ~>
      credentialsRoute.route(selectedVersion, tokenToConnectToUs) ~>
      check {
        status === BadRequest
      }
    }
  }

  "initiateHandshake endpoint" should {
    "send the credentials to them to connect to us" in new CredentialsTestScope {
      val theirVersData =
        s"""
           |{
           |"party_name": "${credsToConnectToThem.businessDetails.name}",
           |"country_code": "${credsToConnectToThem.countryCode}",
           |"party_id": "${credsToConnectToThem.partyId}",
           |"token": "${credsToConnectToThem.token}",
           |"url": "${credsToConnectToThem.url}"
           |}
          """.stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirVersData)

      Post("/initiateHandshake", body) ~> initHandshakeRoute.route ~> check {
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToThem.token)
      }

    }

    "reject indicating the reason if the initiation of the handshake failed" in new CredentialsTestScope {
      val error: HandshakeError = VersionDetailsRetrievalFailed

      handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
        credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url) returns
        Future.successful(-\/(error))

      val theirVersData =
        s"""
           |{
           |"party_name": "${credsToConnectToThem.businessDetails.name}",
           |"country_code": "${credsToConnectToThem.countryCode}",
           |"party_id": "${credsToConnectToThem.partyId}",
           |"token": "${credsToConnectToThem.token}",
           |"url": "${credsToConnectToThem.url}"
           |}
          """.stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirVersData)

      Post("/initiateHandshake", body) ~> initHandshakeRoute.route ~> check {
        responseAs[String].contains(GenericSuccess.code.toString) must beFalse
        responseAs[String] must contain(error.reason)
      }
    }
  }

  trait CredentialsTestScope extends Scope {

    val dateTime = DateTime.parse("2010-01-01T00:00:00Z")

    // their details
    val credsToConnectToThem = Creds(
      token = "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
      url = "https://them.com/ocpi/cpo/versions",
      businessDetails = OcpiBusinessDetails(
        "Example Operator",
        Some(Image("http://them.com/images/logo.png", ImageCategory.Operator, "png")),
        Some("http://them.com")),
      partyId = "EOP",
      countryCode = "NL")

    // our details
    val ourVersionsUrl = "https://us.com/ocpi/msp/versions"
    val selectedVersion = `2.0`
    val tokenToConnectToUs = "aaa3b399-779f-4497-9b9d-ac6ad3cc44aa"
    val credsToConnectToUs = Creds(
      token = tokenToConnectToUs,
      url = ourVersionsUrl,
      businessDetails = OcpiBusinessDetails("Us", None, Some("http://us.com")),
      partyId = "TNM",
      countryCode = "NL")
    val newCredsToConnectToUs = credsToConnectToUs.copy()

    // mock
    val handshakeService = mock[HandshakeService]

    //default mocks
    handshakeService.reactToHandshakeRequest(any, any, any)(any) returns
      Future.successful(\/-(credsToConnectToUs))
    handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
      credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url) returns
      Future.successful(\/-(credsToConnectToThem))
    handshakeService.credsToConnectToUs(any) returns
      -\/(UnknownPartyToken(tokenToConnectToUs))

    val credentialsRoute = new HandshakeRoute(handshakeService, dateTime)
    val initHandshakeRoute = new InitiateHandshakeRoute(handshakeService, dateTime)
  }
}
