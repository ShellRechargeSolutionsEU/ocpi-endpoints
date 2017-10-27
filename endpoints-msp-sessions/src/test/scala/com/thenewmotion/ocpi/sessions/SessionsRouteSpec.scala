package com.thenewmotion.ocpi.sessions

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{SessionId, SessionPatch}
import com.thenewmotion.ocpi.sessions.SessionError.SessionNotFound
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.Future
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

class SessionsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "sessions endpoint" should {

    "accept a new session object without authorizing the operator ID" in new SessionsTestScope {
      val body = HttpEntity(contentType = `application/json`, string = sessionJson1)

      Put("/NL/TNM/SESS1", body) ~> sessionsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beTrue
        there was one(sessionService).createOrUpdateSession(===(apiUser), ===(SessionId("SESS1")), any())(any())
      }
    }

    "accept patches to a session object" in new SessionsTestScope {

      val body = HttpEntity(contentType = `application/json`, string =
        s"""
           |{
           |    "kwh": 8364
           |}
           |""".stripMargin)

      val patch = SessionPatch(kwh = Some(8364))

      Patch("/NL/TNM/SESS1", body) ~> sessionsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beTrue
        there was one(sessionService).updateSession(===(apiUser), ===(SessionId("SESS1")), ===(patch))(any())
      }
    }

    "retrieve a session object" in new SessionsTestScope {
      Get("/NL/TNM/SESS1") ~> sessionsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beTrue
        there was one(sessionService).session(===(apiUser), ===(SessionId("SESS1")))(any())
      }
    }

    "disallow access by authenticated but unauthorized parties" in new SessionsTestScope {
      val body = HttpEntity(contentType = `application/json`, string = "{}")

      Patch("/BE/BEC/SESS1", body) ~> sessionsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection mustEqual AuthorizationFailedRejection
      }
    }
  }

  trait SessionsTestScope extends Scope {
    val sessionService = mock[SessionsService]

    sessionService.createOrUpdateSession(===(GlobalPartyId("NL", "TNM")), ===(SessionId("SESS1")), any())(any()) returns Future(Right(true))
    sessionService.updateSession(===(GlobalPartyId("NL", "TNM")), ===(SessionId("SESS1")), any())(any()) returns Future(Right(()))
    sessionService.session(===(GlobalPartyId("NL", "TNM")), ===(SessionId("SESS1")))(any()) returns Future(Left(SessionNotFound()))

    val apiUser = GlobalPartyId("NL", "TNM")

    val sessionsRoute = new SessionsRoute(sessionService)

    val sessionJson1 =
      """
        |{
        |    "auth_id": "ABC1234",
        |    "auth_method": "AUTH_REQUEST",
        |    "currency": "EUR",
        |    "end_datetime": "2017-03-01T10:00:00Z",
        |    "id": "SESS1",
        |    "kwh": 1000,
        |    "last_updated": "2016-12-31T23:59:59Z",
        |	   "charging_periods": [],
        |    "location": {
        |        "address": "F.Rooseveltlaan 3A",
        |        "charging_when_closed": true,
        |        "city": "Gent",
        |        "coordinates": {
        |            "latitude": "3.729945",
        |            "longitude": "51.047594"
        |        },
        |        "country": "BEL",
        |        "directions": [],
        |        "energy_mix": {
        |            "energy_product_name": "eco-power",
        |            "energy_sources": [],
        |            "environ_impact": [],
        |            "is_green_energy": true,
        |            "supplier_name": "Greenpeace Energy eG"
        |        },
        |        "evses": [
        |            {
        |                "capabilities": [
        |                    "RESERVABLE"
        |                ],
        |                "connectors": [
        |                    {
        |                        "amperage": 16,
        |                        "format": "CABLE",
        |                        "id": "1",
        |                        "last_updated": "2016-12-31T23:59:59Z",
        |                        "power_type": "AC_3_PHASE",
        |                        "standard": "IEC_62196_T2",
        |                        "tariff_id": "kwrate",
        |                        "voltage": 230
        |                    }
        |                ],
        |                "directions": [],
        |                "floor_level": "-1",
        |                "images": [],
        |                "last_updated": "2016-12-31T23:59:59Z",
        |                "parking_restrictions": [],
        |                "physical_reference": "1",
        |                "status": "AVAILABLE",
        |                "status_schedule": [],
        |                "uid": "BE-BEC-E041503001"
        |            }
        |        ],
        |        "facilities": [],
        |        "id": "LOC1",
        |        "images": [],
        |        "last_updated": "2016-12-31T23:59:59Z",
        |        "name": "Gent Zuid",
        |        "postal_code": "9000",
        |        "related_locations": [],
        |        "type": "ON_STREET"
        |    },
        |    "start_datetime": "2017-03-01T08:00:00Z",
        |    "status": "COMPLETED",
        |    "total_cost": 10.24
        |}
      """.stripMargin
  }
}
