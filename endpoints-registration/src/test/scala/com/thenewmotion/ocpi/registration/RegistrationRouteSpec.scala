package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.Specs2RouteTest
import RegistrationError._
import msgs.v2_1.CommonTypes.ImageCategory.Operator
import msgs.v2_1.CommonTypes.{Image, BusinessDetails => OcpiBusinessDetails}
import msgs.v2_1.Credentials.Creds
import msgs.{AuthToken, GlobalPartyId, Url}
import msgs.OcpiStatusCode.GenericSuccess
import msgs.Ownership.{Ours, Theirs}
import msgs.Versions.VersionNumber._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.Future
import cats.syntax.either._

class RegistrationRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.CredentialsJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

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
           |    "party_id": "${credsToConnectToThem.globalPartyId.partyId}",
           |    "country_code": "${credsToConnectToThem.globalPartyId.countryCode}"
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
      registrationService.credsToConnectToUs(any())(any()) returns Future.successful(credsToConnectToUs.asRight)

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
      registrationService.credsToConnectToUs(any())(any()) returns Future.successful(credsToConnectToUs.asRight)
      registrationService.reactToUpdateCredsRequest(any(), any(), any())(any(), any()) returns
        Future.successful(newCredsToConnectToUs.asRight)

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
           |    "party_id": "${credsToConnectToThem.globalPartyId.partyId}",
           |    "country_code": "${credsToConnectToThem.globalPartyId.countryCode}"
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
      registrationService.reactToUpdateCredsRequest(any(), any(), any())(any(), any()) returns
        Future.successful(WaitingForRegistrationRequest(theirGlobalId).asLeft)


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
           |    "party_id": "${credsToConnectToThem.globalPartyId.partyId}",
           |    "country_code": "${credsToConnectToThem.globalPartyId.countryCode}"
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = `application/json`, string = theirNewCredsData)

      Put("/credentials", body) ~>
      credentialsRoute.route(selectedVersion, theirGlobalId) ~>
      check {
        status === MethodNotAllowed
      }
    }
  }

  trait CredentialsTestScope extends Scope {

    val theirGlobalId = GlobalPartyId("NL", "EOP")

    // their details
    val credsToConnectToThem = Creds[Theirs](
      token = AuthToken[Ours]("ebf3b399-779f-4497-9b9d-ac6ad3cc44d2"),
      url = Url("https://them.com/ocpi/cpo/versions"),
      businessDetails = OcpiBusinessDetails(
        "Example Operator",
        Some(Image(Url("http://them.com/images/logo.png"), Operator, "png")),
        Some(Url("http://them.com"))),
      globalPartyId = theirGlobalId)

    // our details
    val ourVersionsUrl = Url("https://us.com/ocpi/msp/versions")
    val selectedVersion = `2.0`
    val tokenToConnectToUs = AuthToken[Theirs]("aaa3b399-779f-4497-9b9d-ac6ad3cc44aa")

    val outGlobalPartyId = GlobalPartyId("NL", "TNM")

    val credsToConnectToUs = Creds[Ours](
      token = tokenToConnectToUs,
      url = ourVersionsUrl,
      businessDetails = OcpiBusinessDetails("Us", None, Some(Url("http://us.com"))),
      globalPartyId = outGlobalPartyId)
    val newCredsToConnectToUs = credsToConnectToUs.copy[Ours](token = AuthToken[Theirs]("abc"))

    // mock
    val registrationService = mock[RegistrationService]

    //default mocks
    registrationService.reactToNewCredsRequest(any(), any(), any())(any(), any()) returns
      Future.successful(credsToConnectToUs.asRight)
    registrationService.initiateRegistrationProcess(credsToConnectToThem.token, tokenToConnectToUs,
      credsToConnectToThem.url) returns Future.successful(credsToConnectToThem.asRight)
    registrationService.credsToConnectToUs(any())(any()) returns Future.successful(UnknownParty(theirGlobalId).asLeft)

    val credentialsRoute = new RegistrationRoute(registrationService)
  }
}
