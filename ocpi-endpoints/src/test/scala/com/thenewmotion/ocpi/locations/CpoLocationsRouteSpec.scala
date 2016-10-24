package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.{ApiUser, JsonApi}
import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_0.Locations.Location
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.{Link, RawHeader}
import spray.http.Uri
import spray.testkit.Specs2RouteTest
import scala.concurrent.Future
import scalaz._


class CpoLocationsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "locations endpoint" should {

    "return paginated list of locations with headers as per OCPI specs" in new LocationsTestScope {
      import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
      import spray.json._

      val InitialClientOffset = 0
      val ClientLimit = 100
      val ServerLimit = 50
      val ResultingLimit = Math.min(ClientLimit, ServerLimit)
      val ServerTotal = 200

      val SecondPageOffset = InitialClientOffset + ResultingLimit
      val ThirdPageOffset = SecondPageOffset + ResultingLimit

      cpoLocService.locations(Pager(InitialClientOffset, ClientLimit)) returns
        Future(\/-(PaginatedResult(List(loc1String.parseJson.convertTo[Location]), ResultingLimit, ServerTotal)))

      Get(s"/?offset=$InitialClientOffset&limit=$ClientLimit") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        response.header[Link].head mustEqual
          Link(Link.Value(Uri(s"http://example.com/?offset=$SecondPageOffset&limit=$ResultingLimit"), Link.next))
        headers.find(_.name == "X-Limit") mustEqual Some(RawHeader("X-Limit", ResultingLimit.toString))
        headers.find(_.name == "X-Total-Count") mustEqual Some(RawHeader("X-Total-Count", ServerTotal.toString))
        there was one(cpoLocService).locations(Pager(InitialClientOffset, ClientLimit))
      }


      cpoLocService.locations(Pager(SecondPageOffset, ResultingLimit)) returns
        Future(\/-(PaginatedResult(List(loc1String.parseJson.convertTo[Location]), ResultingLimit, ServerTotal)))

      Get(s"/?offset=$SecondPageOffset&limit=$ResultingLimit") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        response.header[Link].head mustEqual Link(Link.Value(Uri(s"http://example.com/?offset=$ThirdPageOffset&limit=$ResultingLimit"), Link.next))
        headers.find(_.name == "X-Limit") mustEqual Some(RawHeader("X-Limit", "50"))
        headers.find(_.name == "X-Total-Count") mustEqual Some(RawHeader("X-Total-Count", "200"))
        there was one(cpoLocService).locations(Pager(SecondPageOffset, ResultingLimit))
      }
    }

  }

  trait LocationsTestScope extends Scope with JsonApi{



    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val cpoLocService = mock[CpoLocationsService]


    val apiUser = ApiUser("1", "123", "NL", "TNM")

    val locationsRoute = new CpoLocationsRoute(cpoLocService, dateTime1)

    val loc1String = s"""
                       |{
                       |    "id": "LOC1",
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
                       |    "evses": [{
                       |        "uid": "3256",
                       |        "id": "ICEEVE000123_1",
                       |        "status": "AVAILABLE",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
                       |            "status": "AVAILABLE",
                       |            "standard": "IEC_62196_T2",
                       |            "format": "CABLE",
                       |            "power_type": "AC_3_PHASE",
                       |            "voltage": 220,
                       |            "amperage": 16,
                       |            "tariff_id": "11"
                       |        }, {
                       |            "id": "2",
                       |            "status": "AVAILABLE",
                       |            "standard": "IEC_62196_T2",
                       |            "format": "SOCKET",
                       |            "power_type": "AC_3_PHASE",
                       |            "voltage": 220,
                       |            "amperage": 16,
                       |            "tariff_id": "11"
                       |        }],
                       |        "physical_reference": "1",
                       |        "floor_level": "-1",
                       |        "directions": [],
                       |        "parking_restrictions": [],
                       |        "images": []
                       |    }, {
                       |        "uid": "3257",
                       |        "id": "ICEEVE000123_2",
                       |        "status": "RESERVED",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
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
                       |    }],
                       |    "directions": [],
                       |    "images": [],
                       |    "operator": {
                       |        "name": "The New Motion"
                       |    }
                       |}
                       |""".stripMargin

    val loc2String = loc1String.replace("LOC1","LOC2")

  }
}
