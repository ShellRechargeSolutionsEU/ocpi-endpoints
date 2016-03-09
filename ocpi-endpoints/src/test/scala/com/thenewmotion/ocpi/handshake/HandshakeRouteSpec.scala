package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.handshake.HandshakeError.UnknownPartyToken
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails, Image, ImageCategory}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.{UnableToUseApi, GenericSuccess}
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.MediaTypes._
import spray.http.{ContentType, HttpCharsets, HttpEntity}
import spray.testkit.Specs2RouteTest
import scala.concurrent.Future
import scalaz._

class HandshakeRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "credentials endpoint" should {
    "accept the credentials they sent us to connect to them" in new CredentialsTestScope {
      val theirLog = credsToConnectToThem.business_details.logo.get
      val theirCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.business_details.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.business_details.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.party_id}",
           |    "country_code": "${credsToConnectToThem.country_code}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirCredsData)

      Post("/credentials", body) ~> credentialsRoute.routeWithoutRH(selectedVersion, tokenToConnectToUs) ~> check {
        handled must beTrue
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token)
      }
    }

    "return the credentials we have set for them to connect to us" in new CredentialsTestScope {
      handshakeService.findRegisteredCredsToConnectToUs(any) returns \/-(credsToConnectToUs)

      Get("/credentials") ~> credentialsRoute.routeWithoutRH(selectedVersion, tokenToConnectToUs) ~> check {
        handled must beTrue
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToUs.token)
      }
    }

    "return error if no credentials to connect to us stored for that token" in new CredentialsTestScope {
      Get("/credentials") ~> credentialsRoute.routeWithoutRH(selectedVersion, tokenToConnectToUs) ~> check {
        handled must beFalse
      }
    }

    "accept the update of the credentials they sent us to connect to them" in new CredentialsTestScope {
      handshakeService.findRegisteredCredsToConnectToUs(any) returns \/-(credsToConnectToUs)
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any) returns
        Future.successful(\/-(newCredsToConnectToUs))

      val theirLog = credsToConnectToThem.business_details.logo.get
      val theirNewCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.business_details.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.business_details.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.party_id}",
           |    "country_code": "${credsToConnectToThem.country_code}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirNewCredsData)

      Put("/credentials", body) ~> credentialsRoute.routeWithoutRH(selectedVersion, tokenToConnectToUs) ~> check {
        handled must beTrue
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(newCredsToConnectToUs.token)
      }
    }
    "reject indicating the reason if trying to update credentials for a token we are still waiting for its registration request" in new CredentialsTestScope {
      handshakeService.reactToUpdateCredsRequest(any, any, any)(any) returns
        Future.successful(-\/(WaitingForRegistrationRequest))


      val theirLog = credsToConnectToThem.business_details.logo.get
      val theirNewCredsData =
        s"""
           |{
           |    "token": "${credsToConnectToThem.token}",
           |    "url": "${credsToConnectToThem.url}",
           |    "business_details": {
           |        "name": "${credsToConnectToThem.business_details.name}",
           |        "logo": {
           |          "url": "${theirLog.url}",
           |          "category": "${theirLog.category.name}",
           |          "type": "${theirLog.`type`}"
           |        },
           |        "website": "${credsToConnectToThem.business_details.website.get}"
           |    },
           |    "party_id": "${credsToConnectToThem.party_id}",
           |    "country_code": "${credsToConnectToThem.country_code}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirNewCredsData)

      Put("/credentials", body) ~>  credentialsRoute.routeWithoutRH(selectedVersion, tokenToConnectToUs) ~> check {
        handled must beFalse
      }
    }
  }

  "initiateHandshake endpoint" should {
    "send the credentials to them to connect to us" in new CredentialsTestScope {
      val theirVersData =
        s"""
           |{
           |"token": "${credsToConnectToThem.token}",
           |"url": "${credsToConnectToThem.url}"
           |}
          """.stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirVersData)

      Post("/initiateHandshake", body) ~> initHandshakeRoute.routeWithoutRH ~> check {
        handled must beTrue
        status.isSuccess === true
        responseAs[String] must contain(GenericSuccess.code.toString)
        responseAs[String] must contain(credsToConnectToThem.token)
      }

    }

    "reject indicating the reason if the initiation of the handshake failed" in new CredentialsTestScope {
      val error: HandshakeError = VersionDetailsRetrievalFailed

      handshakeService.initiateHandshakeProcess(credsToConnectToThem.token, credsToConnectToThem.url) returns
        Future.successful(-\/(error))

      val theirVersData =
        s"""
           |{
           |"token": "${credsToConnectToThem.token}",
           |"url": "${credsToConnectToThem.url}"
           |}
          """.stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = theirVersData)

      Post("/initiateHandshake", body) ~> initHandshakeRoute.routeWithoutRH ~> check {
        handled must beFalse
        rejection mustEqual HandshakeErrorRejection(error)
      }

      Post("/initiateHandshake", body) ~> initHandshakeRoute.route ~> check {
        handled must beTrue
        status.isSuccess must beFalse
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
      business_details = OcpiBusinessDetails(
        "Example Operator",
        Some(Image("http://them.com/images/logo.png", ImageCategory.Operator, "png")),
        Some("http://them.com")),
      party_id = "EOP",
      country_code = "NL")

    // our details
    val ourVersionsUrl = "https://us.com/ocpi/msp/versions"
    val selectedVersion = "2.0"
    val tokenToConnectToUs = "aaa3b399-779f-4497-9b9d-ac6ad3cc44aa"
    val credsToConnectToUs = Creds(
      token = tokenToConnectToUs,
      url = ourVersionsUrl,
      business_details = OcpiBusinessDetails("Us", None, Some("http://us.com")),
      party_id = "TNM",
      country_code = "NL")
    val newCredsToConnectToUs = credsToConnectToUs.copy()

    // mock
    val handshakeService = mock[HandshakeService]

    //default mocks
    handshakeService.reactToHandshakeRequest(any, any, any)(any) returns
      Future.successful(\/-(credsToConnectToUs))
    handshakeService.initiateHandshakeProcess(credsToConnectToThem.token, credsToConnectToThem.url) returns
      Future.successful(\/-(credsToConnectToThem))
    handshakeService.findRegisteredCredsToConnectToUs(any) returns
      -\/(UnknownPartyToken(tokenToConnectToUs))

    val credentialsRoute = new HandshakeRoute(handshakeService, dateTime)
    val initHandshakeRoute = new InitiateHandshakeRoute(handshakeService, dateTime)
  }
}
