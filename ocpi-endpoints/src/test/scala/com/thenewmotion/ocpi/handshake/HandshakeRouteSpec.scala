package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
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

      val data =
        s"""
           |{
           |    "token": "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
           |    "url": "https://example.com/ocpi/cpo/",
           |    "business_details": {
           |        "name": "Example Operator",
           |        "logo": "http://example.com/images/logo.png",
           |        "website": "http://example.com"
           |    }
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = data)

      Post("/credentials", body) ~> credentialsRoute.route("2.0", "123") ~> check {
        handled must beTrue
      }
    }

    "initiateHandshake endpoint" should {
      "send the credentials to them to connect to us" in new CredentialsTestScope {
        val data =
          s"""
             |{
             |"auth": "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
             |"url": "https://example.com/ocpi/cpo/versions"
             |}
          """.stripMargin

        val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = data)

        Post("/initiateHandshake", body) ~> credentialsRoute.route("2.0", "123") ~> check {
          handled must beTrue
        }

      }
    }
  }

  trait CredentialsTestScope extends Scope {

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")
    val theirCredentials = Creds(
      token = "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
      url = "https://example.com/ocpi/cpo/",
      business_details = OcpiBusinessDetails(
        "Example Operator",
        Some("http://example.com/images/logo.png"),
        Some("http://example.com")
      )
    )

    val handshakeService = mock[HandshakeService]
    handshakeService.reactToHandshakeRequest(any, any, any, any)(any) returns
      Future.successful(\/-(theirCredentials))

    val credentialsRoute = new HandshakeRoute(handshakeService, "https://example.com/ocpi/cpo/", dateTime1)
  }
}
