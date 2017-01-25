package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl.model.headers.{Link, RawHeader}
import common.{Pager, PaginatedResult}
import msgs.v2_1.CommonTypes.{SuccessWithDataResp, ErrorResp}
import msgs.v2_1.Tokens._
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.Future
import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
import scalaz._
import msgs.v2_1.OcpiStatusCode._

class MspTokensRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "MspTokensRoute" should {
    "return a paged set of Tokens" in new TestScope {
      service.tokens(Pager(0, 1000), None, None) returns Future(PaginatedResult(List(token), 1))
      Get() ~> route.route(apiUser) ~> check {
        header[Link] must beNone
        headers.find(_.name == "X-Limit") mustEqual Some(RawHeader("X-Limit", "1000"))
        headers.find(_.name == "X-Total-Count") mustEqual Some(RawHeader("X-Total-Count", "1"))
        there was one(service).tokens(Pager(0, 1000), None, None)
        val res = entityAs[SuccessWithDataResp[List[Token]]]
        res.data mustEqual List(token)
      }
    }

    "authorize without location references" in new TestScope {
      service.authorize("23455655A", None) returns Future(\/-(AuthorizationInfo(Allowed.Allowed)))

      Post("/23455655A/authorize") ~> route.route(apiUser) ~> check {
        there was one(service).authorize("23455655A", None)
        val res = entityAs[SuccessWithDataResp[AuthorizationInfo]]
        res.data.allowed mustEqual Allowed.Allowed
      }
    }

    "authorize with location references" in new TestScope {

      val lr = LocationReferences("1234", List("1234"), List("1234", "5678"))

      service.authorize("23455655A", Some(lr)) returns Future(\/-(AuthorizationInfo(Allowed.Allowed)))

      Post("/23455655A/authorize", lr) ~> route.route(apiUser) ~> check {
        there was one(service).authorize("23455655A", Some(lr))
        val res = entityAs[SuccessWithDataResp[AuthorizationInfo]]
        res.data.allowed mustEqual Allowed.Allowed
      }
    }

    "handle authorize failure" in new TestScope {
      service.authorize("23455655A", None) returns Future(-\/(MustProvideLocationReferences))

      Post("/23455655A/authorize") ~> route.route(apiUser) ~> check {
        there was one(service).authorize("23455655A", None)
        val res = entityAs[ErrorResp]
        res.statusCode mustEqual NotEnoughInformation
      }
    }
  }

  trait TestScope extends Scope with JsonApi {
    val apiUser = ApiUser("1", "123", "NL", "TNM")

    val token = Token(
      uid = "23455655A",
      `type` = TokenType.Rfid,
      authId = "NL-TNM-000660755-V",
      visualNumber = Some("NL-TNM-066075-5"),
      issuer = "TheNewMotion",
      valid = true,
      whitelist = WhitelistType.Allowed,
      lastUpdated = DateTime.parse("2017-01-24T10:00:00.000Z")
    )

    val service = mock[MspTokensService]

    val route = new MspTokensRoute(service)
  }
}
