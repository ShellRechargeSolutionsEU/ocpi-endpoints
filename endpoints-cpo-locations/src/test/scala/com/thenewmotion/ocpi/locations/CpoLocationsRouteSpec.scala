package com.thenewmotion.ocpi.locations

import java.time.ZonedDateTime

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.{Link, LinkParams, RawHeader}
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.JsonApi
import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{Connector, Evse, Location, LocationId, EvseUid, ConnectorId}
import scala.concurrent.Future
import spray.json._

class CpoLocationsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import com.thenewmotion.ocpi.msgs.v2_1.LocationsJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

  "locations endpoint" should {

    "return paginated list of locations with headers as per OCPI specs" in new LocationsTestScope {

      val InitialClientOffset = 0
      val ClientPageLimit = 100

      val ResultingPageLimit = Math.min(ClientPageLimit, ServerPageLimit)
      val ServerTotal = 200

      val SecondPageOffset = InitialClientOffset + ResultingPageLimit
      val ThirdPageOffset = SecondPageOffset + ResultingPageLimit

      cpoLocService.locations(Pager(InitialClientOffset, ResultingPageLimit), None, None) returns
        Future(Right(PaginatedResult(List(loc1String.parseJson.convertTo[Location]), ServerTotal)))

      Get(s"/?offset=$InitialClientOffset&limit=$ClientPageLimit") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        response.header[Link].head mustEqual
          Link(Uri(s"http://example.com/?offset=$SecondPageOffset&limit=$ResultingPageLimit"), LinkParams.next)
        headers.find(_.name == "X-Limit") mustEqual Some(RawHeader("X-Limit", ResultingPageLimit.toString))
        headers.find(_.name == "X-Total-Count") mustEqual Some(RawHeader("X-Total-Count", ServerTotal.toString))
        there was one(cpoLocService).locations(Pager(InitialClientOffset, ResultingPageLimit), None, None)
      }


      cpoLocService.locations(Pager(SecondPageOffset, ResultingPageLimit), None, None) returns
        Future(Right(PaginatedResult(List(loc1String.parseJson.convertTo[Location]), ServerTotal)))

      Get(s"/?offset=$SecondPageOffset&limit=$ResultingPageLimit") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        response.header[Link].head mustEqual Link(Uri(s"http://example.com/?offset=$ThirdPageOffset&limit=$ResultingPageLimit"), LinkParams.next)
        headers.find(_.name == "X-Limit") must beSome(RawHeader("X-Limit", "50"))
        headers.find(_.name == "X-Total-Count") must beSome(RawHeader("X-Total-Count", "200"))
        there was one(cpoLocService).locations(Pager(SecondPageOffset, ResultingPageLimit), None, None)
      }
    }

    "accept date_from and date_to" in new LocationsTestScope {

      cpoLocService.locations(any(), any(), any()) returns
        Future(Right(PaginatedResult(List(loc1String.parseJson.convertTo[Location]), 1000)))

      val dateFrom = "2015-06-29T20:39:09Z"
      val dateTo = "2016-06-29T20:39:09Z"
      Get(s"/?date_from=$dateFrom&date_to=$dateTo") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(cpoLocService).locations(Pager(0, ServerPageLimit), Some(ZonedDateTime.parse(dateFrom)), Some(ZonedDateTime.parse(dateTo)))
      }
    }

    "retrieve a single..." >> {

      "location object" in new LocationsTestScope {
        Get("/LOC1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
          there was one(cpoLocService).location(===(LocationId("LOC1")))
        }
      }

      "EVSE object" in new LocationsTestScope {
        Get("/LOC1/3256") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
          there was one(cpoLocService).evse(===(LocationId("LOC1")), ===(EvseUid("3256")))
        }
      }

      "connector object" in new LocationsTestScope {
        Get("/LOC1/3256/1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
          there was one(cpoLocService).connector(===(LocationId("LOC1")), ===(EvseUid("3256")), ===(ConnectorId("1")))
        }
      }
    }
  }

  trait LocationsTestScope extends Scope with JsonApi {

    val ZonedDateTime1 = ZonedDateTime.parse("2010-01-01T00:00:00Z")

    val cpoLocService = mock[CpoLocationsService]

    val apiUser = GlobalPartyId("NL", "TNM")

    val ServerPageLimit = 50
    val locationsRoute = new CpoLocationsRoute(cpoLocService, ServerPageLimit, ServerPageLimit, currentTime = ZonedDateTime1)

    val evse1conn1String =
      s"""
         |{
         |            "id": "1",
         |            "last_updated": "2016-12-31T23:59:59Z",
         |            "status": "AVAILABLE",
         |            "standard": "IEC_62196_T2",
         |            "format": "CABLE",
         |            "power_type": "AC_3_PHASE",
         |            "voltage": 220,
         |            "amperage": 16,
         |            "tariff_id": "11"
         | }
       """.stripMargin

    val evse1conn2String =
      s"""
         |{
         |            "id": "2",
         |            "last_updated": "2016-12-31T23:59:59Z",
         |            "status": "AVAILABLE",
         |            "standard": "IEC_62196_T2",
         |            "format": "SOCKET",
         |            "power_type": "AC_3_PHASE",
         |            "voltage": 220,
         |            "amperage": 16,
         |            "tariff_id": "11"
         | }
       """.stripMargin

    val evse1String =
      s"""
         |{
         |        "uid": "3256",
         |        "last_updated": "2016-12-31T23:59:59Z",
         |        "evse_id": "ICEEVE000123_1",
         |        "status": "AVAILABLE",
         |        "status_schedule": [],
         |        "capabilities": [
         |            "RESERVABLE"
         |        ],
         |        "connectors": [$evse1conn1String, $evse1conn2String],
         |        "physical_reference": "1",
         |        "floor_level": "-1",
         |        "directions": [],
         |        "parking_restrictions": [],
         |        "images": []
         |
         | }
       """.stripMargin

    val evse2String =
      s"""
         | {
         |        "uid": "3257",
         |        "last_updated": "2016-12-31T23:59:59Z",
         |        "evse_id": "ICEEVE000123_2",
         |        "status": "RESERVED",
         |        "status_schedule": [],
         |        "capabilities": [
         |            "RESERVABLE"
         |        ],
         |        "connectors": [{
         |            "id": "1",
         |            "last_updated": "2016-12-31T23:59:59Z",
         |            "status": "AVAILABLE",
         |            "standard": "IEC_62196_T2",
         |            "format": "SOCKET",
         |            "power_type": "AC_3_PHASE",
         |            "voltage": 220,
         |            "amperage": 16,
         |            "tariff_id": "12"
         |        }],
         |        "physical_reference": "2",
         |        "floor_level": "-2",
         |        "directions": [],
         |        "parking_restrictions": [],
         |        "images": []
         | }
       """.stripMargin

    val loc1String = s"""
                       |{
                       |    "id": "LOC1",
                       |    "last_updated": "2016-12-31T23:59:59Z",
                       |    "type": "ON_STREET",
                       |    "name": "Keizersgracht 585",
                       |    "address": "Keizersgracht 585",
                       |    "city": "Amsterdam",
                       |    "postal_code": "1017DR",
                       |    "country": "NLD",
                       |    "coordinates": {
                       |        "latitude": "52.364115",
                       |        "longitude": "4.891733"
                       |    },
                       |    "related_locations": [],
                       |    "evses": [$evse1String, $evse2String],
                       |    "directions": [],
                       |    "images": [],
                       |    "facilities": [],
                       |    "operator": {
                       |        "name": "The New Motion"
                       |    }
                       |}
                       |""".stripMargin

    val loc2String = loc1String.replace("LOC1","LOC2")

    cpoLocService.location(LocationId("LOC1")) returns
      Future(Right(loc1String.parseJson.convertTo[Location]))

    cpoLocService.evse(LocationId("LOC1"), EvseUid("3256")) returns
      Future(Right(evse1String.parseJson.convertTo[Evse]))

    cpoLocService.connector(LocationId("LOC1"), EvseUid("3256"), ConnectorId("1")) returns
      Future(Right(evse1conn1String.parseJson.convertTo[Connector]))
  }
}
