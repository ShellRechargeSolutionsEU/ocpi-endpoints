package com.thenewmotion.ocpi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, GenericHttpCredentials, RawHeader}
import akka.http.scaladsl.testkit.Specs2RouteTest
import cats.effect.IO
import com.thenewmotion.ocpi.VersionsRoute.OcpiVersionConfig
import com.thenewmotion.ocpi.common.{OcpiDirectives, OcpiRejectionHandler, TokenAuthenticator}
import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import com.thenewmotion.ocpi.msgs.Versions.{EndpointIdentifier, VersionNumber}
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.json._
import spray.json.lenses.JsonLenses._

class VersionsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

  "Versions Route" should {
    "authenticate api calls with valid token info" in new VersionsScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> testRoute ~> check {
        handled must beTrue
      }
    }

    "return error for api calls without Authorization header" in new VersionsScope {
      Get("/cpo/versions") ~>
        addHeader(invalidHeaderName) ~> testRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int](Symbol("status_code")) mustEqual 2011
      }
    }

    "return error for api calls without valid token" in new VersionsScope {
      Get("/cpo/versions") ~>
      addHeader(invalidToken) ~>
      testRoute ~>
      check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int](Symbol("status_code")) mustEqual 2010
      }
    }

    // ----------------------------------------------------------------

    "route calls to our versions endpoint" in new VersionsScope {
      Get("/cpo/versions") ~>
        addHeader(validToken) ~> testRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int](Symbol("status_code")) mustEqual 1000
        json.extract[String](Symbol("data") / * / Symbol("version")) mustEqual List("2.1")
        json.extract[String](Symbol("data") / * / Symbol("url")) mustEqual List("http://example.com/cpo/versions/2.1")
      }
    }

    "route calls to our version details" in new VersionsScope {
      Get("/cpo/versions/2.1") ~>
        addHeader(validToken) ~> testRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int](Symbol("status_code")) mustEqual 1000
        json.extract[String](Symbol("data") / Symbol("version")) mustEqual "2.1"
        json.extract[String](Symbol("data") / Symbol("endpoints") / * / Symbol("identifier")) mustEqual List("credentials", "locations", "tokens")
        json.extract[String](Symbol("data") / Symbol("endpoints") / * / Symbol("url")) mustEqual
          List(
            "http://example.com/cpo/versions/2.1/credentials",
            "http://example.com/cpo/versions/2.1/locations",
            "http://example.com/cpo/versions/2.1/tokens"
          )
      }
    }

    "route calls to our version details when terminated by slash" in new VersionsScope {
      Get("/cpo/versions/2.1/") ~>
        addHeader(validToken) ~> testRoute ~> check {
        handled must beTrue

        val json = responseAs[String].parseJson
        json.extract[Int](Symbol("status_code")) mustEqual 1000
        json.extract[String](Symbol("data") / Symbol("version")) mustEqual "2.1"
        json.extract[String](Symbol("data") / Symbol("endpoints") / * / Symbol("identifier")) mustEqual List("credentials", "locations", "tokens")
        json.extract[String](Symbol("data") / Symbol("endpoints") / * / Symbol("url")) mustEqual
          List(
            "http://example.com/cpo/versions/2.1/credentials",
            "http://example.com/cpo/versions/2.1/locations",
            "http://example.com/cpo/versions/2.1/tokens"
          )
      }
    }
  }

  trait VersionsScope extends Scope with OcpiDirectives with SprayJsonSupport {
    val validToken = Authorization(GenericHttpCredentials("Token", "12345"))
    val invalidHeaderName = RawHeader("Auth", "Token 12345")
    val invalidToken = Authorization(GenericHttpCredentials("Token", "letmein"))

    val ourCredentialsRoute = (version: VersionNumber, apiUser: GlobalPartyId) => complete((StatusCodes.OK, s"credentials: $version"))
    val ourLocationsRoute = (version: VersionNumber, apiUser: GlobalPartyId) => complete((StatusCodes.OK, s"locations: $version"))
    val ourTokensRoute = (version: VersionNumber, apiUser: GlobalPartyId) => complete((StatusCodes.OK, s"tokens: $version"))

    val versions: Map[VersionNumber, OcpiVersionConfig] = Map (
      VersionNumber.`2.1` -> OcpiVersionConfig(
        endPoints = Map(
          EndpointIdentifier.Credentials -> Right(ourCredentialsRoute),
          EndpointIdentifier.Locations -> Right(ourLocationsRoute),
          EndpointIdentifier.Tokens -> Right(ourTokensRoute)
        )
      )
    )

    // TODO Testing of TokenAuthenticator should be somewhere else
    val auth = new TokenAuthenticator(theirToken =>
      IO.pure {
        if (theirToken == AuthToken[Theirs]("12345")) Some(GlobalPartyId("BE", "BEC")) else None
      }
    )

    import com.thenewmotion.ocpi.common.HktMarshallableFromECInstances._
    val versionsRoute = VersionsRoute(IO.pure(versions))

    val testRoute =
      (pathPrefix("cpo") & pathPrefix("versions")) {
        handleRejections(OcpiRejectionHandler.Default) {
          authenticateOrRejectWithChallenge(auth) { apiUser =>
            versionsRoute(apiUser, securedConnection = false)
          }
        }
      }
  }
}
