package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_0.Versions.EndpointIdentifier
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http.StatusCodes
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, MissingHeaderRejection}
import spray.testkit.Specs2RouteTest
import org.joda.time.DateTime
import spray.json._, lenses.JsonLenses._
import spray.json.DefaultJsonProtocol._

class TopLevelRouteSpec extends Specification with Specs2RouteTest with Mockito{

  "api" should {
    "extract the token value from the header value" in {
      val auth = new Authenticator(user => None)
      auth.extractTokenValue("Basic 12345") must beNone
      auth.extractTokenValue("Token 12345") mustEqual Some("12345")
      auth.extractTokenValue("Token ") must beNone
    }

    "authenticate api calls with valid token info" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> topLevelRoute.route ~> check {
        handled must beTrue
      }
    }

    "reject api calls without Authorization header" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(invalidHeaderName) ~> topLevelRoute.route ~> check {
        handled must beFalse
        rejections must contain(MissingHeaderRejection("Authorization"))
      }
    }

    "reject api calls without valid token" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(invalidToken) ~> topLevelRoute.route ~> check {
        handled must beFalse
        rejections must contain(AuthenticationFailedRejection(CredentialsRejected,List()))
      }
    }

    // ----------------------------------------------------------------

    "route calls to our versions endpoint" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> topLevelRoute.route ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / * / 'version) mustEqual List("2.0")
        json.extract[String]('data / * / 'url) mustEqual List("http://example.com/cpo/versions/2.0")
      }
    }

    "route calls to our version details" in new TopLevelScope {
      Get("/cpo/versions/2.0") ~>
        addHeader(validToken) ~> topLevelRoute.route ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / 'version) mustEqual "2.0"
        json.extract[String]('data / 'endpoints / * / 'identifier) mustEqual List("credentials", "locations")
        json.extract[String]('data / 'endpoints / * / 'url) mustEqual
          List("http://example.com/cpo/versions/2.0/credentials", "http://example.com/cpo/versions/2.0/locations")
      }
    }

    "route calls to our version details when terminated by slash" in new TopLevelScope {
      Get("/cpo/versions/2.0/") ~>
        addHeader(validToken) ~> topLevelRoute.route ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / 'version) mustEqual "2.0"
        json.extract[String]('data / 'endpoints / * / 'identifier) mustEqual List("credentials", "locations")
        json.extract[String]('data / 'endpoints / * / 'url) mustEqual
          List("http://example.com/cpo/versions/2.0/credentials", "http://example.com/cpo/versions/2.0/locations")
      }
    }
  }

  trait TopLevelScope extends Scope with JsonApi {
    val validToken = RawHeader("Authorization", "Token 12345")
    val invalidHeaderName = RawHeader("Auth", "Token 12345")
    val invalidToken = RawHeader("Authorization", "Token letmein")

    val ourCredentialsRoute = (version: String, token: String) => complete((StatusCodes.OK, s"credentials: $version"))
    val ourLocationsRoute = (version: String, token: String) => complete((StatusCodes.OK, s"locations: $version"))

    val topLevelRoute = new TopLevelRoute {

      override val currentTime = DateTime.parse("2010-01-01T00:00:00Z")

      override val routingConfig = OcpiRoutingConfig("cpo", "versions",
        Map (
          "2.0" -> OcpiVersionConfig(
            endPoints = Map(
              EndpointIdentifier.Credentials -> ourCredentialsRoute,
              EndpointIdentifier.Locations -> ourLocationsRoute
            )
          )
        )
      ) {
        token => if (token == "12345") Some(ApiUser("beCharged","12345")) else None
      }
    }
  }
}
