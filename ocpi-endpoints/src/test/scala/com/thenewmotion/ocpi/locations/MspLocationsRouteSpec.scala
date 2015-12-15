package com.thenewmotion.ocpi.locations


import com.thenewmotion.ocpi.ApiUser
import com.thenewmotion.ocpi.locations.LocationsError._
import org.joda.time.DateTime
import org.mockito.Matchers
import Matchers.{eq => eq_}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.MediaTypes._
import spray.http.{ContentType, HttpCharsets, HttpEntity}
import spray.routing.{AuthorizationFailedRejection, MalformedRequestContentRejection}
import spray.testkit.Specs2RouteTest
import scalaz._


class MspLocationsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  "locations endpoint" should {

    "accept a new location object without authorizing the location ID" in new LocationsTestScope {

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`),
        string = loc2String)

      Put("/NL/TNM/LOC2", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).createLocation(eq_(CpoId("NL", "TNM")), eq_("LOC2"), any)
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
        there was one(mspLocService).updateLocation(eq_(CpoId("NL", "TNM")), eq_("LOC1"), any)
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
           |    "id": "NL-TNM-02000000",
           |    "status": "CHARGING"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1/NL-TNM-02000000", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateEvse(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), any)
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
        there was one(mspLocService).updateConnector(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), eq_("1"), any)
      }
    }

    "retrieve a location object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).location(eq_(CpoId("NL", "TNM")), eq_("LOC1"))
      }
    }

    "retrieve a EVSE object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).evse(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"))
      }
    }

    "retrieve a connector object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000/1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).connector(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), eq_("1"))
      }
    }

    "disallow access by authenticated but unauthorized parties" in new LocationsTestScope {
      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = "{}")

      Patch("/BE/BEC/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection must beLike { case AuthorizationFailedRejection => ok }
      }
    }

    "disallow unauthorized resource access" in new LocationsTestScope {
      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = "{}")

      Patch("/NL/TNM/LOC2", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection must beLike { case AuthorizationFailedRejection => ok }
      }
    }
  }

  trait LocationsTestScope extends Scope {

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    def authorizeAccess(cc: String, opId: String, locId: String) =
      (cc, opId, locId) match {
        case ("NL", "TNM", "LOC1") => true
        case _ => false
      }


    val mspLocService = mock[MspLocationsService]

    mspLocService.createLocation(eq_(CpoId("NL", "TNM")), eq_("LOC2"), any) returns \/-(Unit)
    mspLocService.updateLocation(eq_(CpoId("NL", "TNM")), eq_("LOC1"), any) returns \/-(Unit)
    mspLocService.location(eq_(CpoId("NL", "TNM")), eq_("LOC1")) returns -\/(LocationRetrievalFailed())
    mspLocService.evse(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000")) returns -\/(LocationRetrievalFailed())
    mspLocService.connector(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), eq_("1")) returns -\/(LocationRetrievalFailed())
    mspLocService.updateEvse(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), any) returns \/-(Unit)
    mspLocService.updateConnector(eq_(CpoId("NL", "TNM")), eq_("LOC1"), eq_("NL-TNM-02000000"), eq_("1"),any) returns \/-(Unit)

    val apiUser = ApiUser("1", "123", "NL", "TNM")

    val locationsRoute = new MspLocationsRoute(mspLocService, authorizeAccess, dateTime1)

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
