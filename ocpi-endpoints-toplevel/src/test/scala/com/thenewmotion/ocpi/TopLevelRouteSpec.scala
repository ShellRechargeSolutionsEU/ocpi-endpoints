package com.thenewmotion.ocpi

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, RawHeader}
import com.thenewmotion.ocpi.handshake.HandshakeService
import com.thenewmotion.ocpi.msgs.v2_1.Versions.{EndpointIdentifier, VersionNumber}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.joda.time.DateTime
import spray.json._
import lenses.JsonLenses._
import spray.json.DefaultJsonProtocol._

class TopLevelRouteSpec extends Specification with Specs2RouteTest with Mockito{

  "api" should {
    "authenticate api calls with valid token info" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> topLevelRoute.topLevelRoute ~> check {
        handled must beTrue
      }
    }

    "return error for api calls without Authorization header" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(invalidHeaderName) ~> topLevelRoute.topLevelRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 2011
      }
    }

    "return error for api calls without valid token" in new TopLevelScope {
      Get("/cpo/versions") ~>
      addHeader(invalidToken) ~>
      topLevelRoute.topLevelRoute ~>
      check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 2010
      }
    }

    // ----------------------------------------------------------------

    "route calls to our versions endpoint" in new TopLevelScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> topLevelRoute.topLevelRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / * / 'version) mustEqual List("2.0")
        json.extract[String]('data / * / 'url) mustEqual List("http://example.com/cpo/versions/2.0")
      }
    }

    "route calls to our version details" in new TopLevelScope {
      Get("/cpo/versions/2.0") ~>
        addHeader(validToken) ~> topLevelRoute.topLevelRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / 'version) mustEqual "2.0"
        json.extract[String]('data / 'endpoints / * / 'identifier) mustEqual List("credentials", "locations", "tokens")
        json.extract[String]('data / 'endpoints / * / 'url) mustEqual
          List(
            "http://example.com/cpo/versions/2.0/credentials",
            "http://example.com/cpo/versions/2.0/locations",
            "http://example.com/cpo/versions/2.0/tokens"
          )
      }
    }

    "route calls to our version details when terminated by slash" in new TopLevelScope {
      Get("/cpo/versions/2.0/") ~>
        addHeader(validToken) ~> topLevelRoute.topLevelRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int]('status_code) mustEqual 1000
        json.extract[String]('data / 'version) mustEqual "2.0"
        json.extract[String]('data / 'endpoints / * / 'identifier) mustEqual List("credentials", "locations", "tokens")
        json.extract[String]('data / 'endpoints / * / 'url) mustEqual
          List(
            "http://example.com/cpo/versions/2.0/credentials",
            "http://example.com/cpo/versions/2.0/locations",
            "http://example.com/cpo/versions/2.0/tokens"
          )
      }
    }
  }

  trait TopLevelScope extends Scope with JsonApi {
    val validToken = Authorization(GenericHttpCredentials("Token", "12345"))
    val invalidHeaderName = RawHeader("Auth", "Token 12345")
    val invalidToken = Authorization(GenericHttpCredentials("Token", "letmein"))

    val ourCredentialsRoute = (version: VersionNumber, apiUser: ApiUser) => complete((StatusCodes.OK, s"credentials: $version"))
    val ourLocationsRoute = (version: VersionNumber, apiUser: ApiUser) => complete((StatusCodes.OK, s"locations: $version"))
    val ourTokensRoute = (version: VersionNumber, apiUser: ApiUser) => complete((StatusCodes.OK, s"tokens: $version"))
    val mockHandshakeService = mock[HandshakeService]
    val topLevelRoute = new TopLevelRoute {

      override val currentTime = DateTime.parse("2010-01-01T00:00:00Z")

      override val routingConfig = OcpiRoutingConfig("cpo",
        Map (
          "2.0" -> OcpiVersionConfig(
            endPoints = Map(
              EndpointIdentifier.Credentials -> Right(ourCredentialsRoute),
              EndpointIdentifier.Locations -> Right(ourLocationsRoute),
              EndpointIdentifier.Tokens -> Right(ourTokensRoute)
            )
          )
        ), mockHandshakeService
      ) { token => if (token == "12345") Some(ApiUser("beCharged","12345", "BE", "BEC")) else None }
        { token => if (token == "initiate") Some(ApiUser("admin", "initiate", "BE", "BEC")) else None }
    }
  }
}
