package com.thenewmotion.ocpi.common

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Forbidden, Unauthorized}
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.msgs
import com.thenewmotion.ocpi.msgs.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import msgs.sprayjson.v2_1.protocol._

class OcpiRejectionHandlerSpec extends Specification with Specs2RouteTest with Mockito {

  "OcpiRejectionHandler" should {
    "handle Malformed Content" in new TestScope {
      Get() ~> route(MalformedRequestContentRejection("Something is wrong", new RuntimeException("Ooopsie"))) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual GenericClientFailure
        res.statusMessage must beSome("Something is wrong")
        status mustEqual BadRequest
      }
    }

    "handle missing credentials header" in new TestScope {
      Get() ~> route(
        AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsMissing, HttpChallenge("blah", "blah"))
      ) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual MissingHeader
        res.statusMessage must beSome("Authorization Token not supplied")
        status mustEqual Unauthorized
      }
    }

    "handle wrong credentials" in new TestScope {
      Get() ~> route(
        AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, HttpChallenge("blah", "blah"))
      ) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual AuthenticationFailed
        res.statusMessage must beSome("Invalid Authorization Token")
        status mustEqual Unauthorized
      }
    }

    "handle not authorized" in new TestScope {
      Get() ~> route(
        AuthorizationFailedRejection
      ) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual GenericClientFailure
        res.statusMessage must beSome("The client is not authorized to access /")
        status mustEqual Forbidden
      }
    }

    "handle missing header" in new TestScope {
      Get() ~> route(
        MissingHeaderRejection("monkeys")
      ) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual MissingHeader
        res.statusMessage must beSome("Header not found: 'monkeys'")
        status mustEqual BadRequest
      }
    }

    "handle other rejections" in new TestScope {
      Get() ~> route(
        MissingCookieRejection("xyz"), UnsupportedWebSocketSubprotocolRejection("abc")
      ) ~> check {
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual GenericClientFailure
        res.statusMessage must beSome("MissingCookieRejection(xyz), UnsupportedWebSocketSubprotocolRejection(abc)")
        status mustEqual BadRequest
      }
    }
  }

  trait TestScope extends Scope with OcpiDirectives with SprayJsonSupport {
    def route(rejections: Rejection*): Route =
      handleRejections(OcpiRejectionHandler.Default) {
        reject(rejections: _*)
      }
  }
}
