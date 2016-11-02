package com.thenewmotion.ocpi.locations

import com.thenewmotion.mobilityid.{CountryCode, OperatorId}
import com.thenewmotion.ocpi.ApiUser
import com.thenewmotion.ocpi.locations.LocationsError.LocationNotFound
import com.thenewmotion.spray.testkit.Specs2RouteTest
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.MediaTypes._
import spray.http.{ContentType, HttpCharsets, HttpEntity}
import spray.routing.{AuthorizationFailedRejection, MalformedRequestContentRejection}
import scala.concurrent.Future
import scalaz._

class MspLocationsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "locations endpoint" should {

    "accept a new location object without authorizing the location ID" in new LocationsTestScope {

      import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
      import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
      import spray.json._

      loc2String.parseJson.convertTo[Location]
      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`),
        string = loc2String)

      Put("/NL/TNM/LOC2", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).createOrUpdateLocation(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC2"), any)
      }
    }

    "accept patches to a location object" in new LocationsTestScope {

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string =
        s"""
           |{
           |    "id": "LOC1",
           |    "address": "Otherstreet 12"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateLocation(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), any)
      }
    }

    "refuse patches to a location object with malformed JSON" in new LocationsTestScope {

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string =
        s"""
           |{
           |    "UID": "LOC1",
           |    "address": "Otherstreet 12"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection must beLike { case MalformedRequestContentRejection(msg, _) if msg.contains("'id'")=> ok }
      }
    }

    "accept patches to an EVSE object" in new LocationsTestScope {

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string =
        s"""
           |{
           |    "uid": "NL-TNM-02000000",
           |    "status": "CHARGING"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1/NL-TNM-02000000", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateEvse(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), any)
      }
    }

    "accept patches to a connector object" in new LocationsTestScope {

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string =
        s"""
           |{
           |    "id": "1",
           |    "status": "CHARGING"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1/NL-TNM-02000000/1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateConnector(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), ===("1"), any)
      }
    }

    "retrieve a location object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).location(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"))
      }
    }

    "retrieve a EVSE object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).evse(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"))
      }
    }

    "retrieve a connector object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000/1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).connector(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), ===("1"))
      }
    }

    "disallow access by authenticated but unauthorized parties" in new LocationsTestScope {
      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = "{}")

      Patch("/BE/BEC/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection mustEqual AuthorizationFailedRejection
      }
      Patch("/NL/BEC/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection mustEqual AuthorizationFailedRejection
      }
      Patch("/BE/TNM/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection mustEqual AuthorizationFailedRejection
      }
    }
  }

  trait LocationsTestScope extends Scope {

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val mspLocService = mock[MspLocationsService]

    mspLocService.createOrUpdateLocation(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC2"), any) returns Future(\/-(true))
    mspLocService.updateLocation(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), any) returns Future(\/-(Unit))
    mspLocService.location(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1")) returns Future(-\/(LocationNotFound()))
    mspLocService.evse(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000")) returns Future(-\/(LocationNotFound()))
    mspLocService.connector(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), ===("1")) returns Future(-\/(LocationNotFound()))
    mspLocService.updateEvse(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), any) returns Future(\/-(Unit))
    mspLocService.updateConnector(===(CountryCode("NL")), ===(OperatorId("TNM")), ===("LOC1"), ===("NL-TNM-02000000"), ===("1"),any) returns Future(\/-(Unit))

    val apiUser = ApiUser("1", "123", "NL", "TNM")

    val locationsRoute = new MspLocationsRoute(mspLocService, dateTime1)

    val loc1String = s"""
                       |{
                       |    "id": "LOC1",
                       |    "last_updated": "2014-06-25T00:00:00+02:00",
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
                       |        "last_updated": "2014-06-25T00:00:00+02:00",
                       |        "id": "ICEEVE000123_1",
                       |        "status": "AVAILABLE",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
                       |            "last_updated": "2014-06-25T00:00:00+02:00",
                       |            "status": "AVAILABLE",
                       |            "standard": "IEC_62196_T2",
                       |            "format": "CABLE",
                       |            "power_type": "AC_3_PHASE",
                       |            "voltage": 220,
                       |            "amperage": 16,
                       |            "tariff_id": "11"
                       |        }, {
                       |            "id": "2",
                       |            "last_updated": "2014-06-25T00:00:00+02:00",
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
                       |        "last_updated": "2014-06-25T00:00:00+02:00",
                       |        "id": "ICEEVE000123_2",
                       |        "status": "RESERVED",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
                       |            "last_updated": "2014-06-25T00:00:00+02:00",
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
                       |    "facilities":[],
                       |    "operator": {
                       |        "name": "The New Motion"
                       |    }
                       |}
                       |""".stripMargin

    val loc2String = loc1String.replace("LOC1","LOC2")

  }
}
