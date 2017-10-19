package com.thenewmotion.ocpi.locations

import akka.http.scaladsl.model.HttpEntity
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.Future
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, MalformedRequestContentRejection}
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.thenewmotion.ocpi.locations.LocationsError.LocationNotFound
import com.thenewmotion.ocpi.msgs
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{ConnectorId, EvseUid, LocationId}

class MspLocationsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.LocationsJsonProtocol._

  "locations endpoint" should {

    "accept a new location object without authorizing the operator ID" in new LocationsTestScope {

      import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
      import msgs.v2_1.DefaultJsonProtocol._
      import msgs.v2_1.LocationsJsonProtocol._
      import spray.json._

      loc2String.parseJson.convertTo[Location]
      val body = HttpEntity(contentType = `application/json`, string = loc2String)

      Put("/NL/TNM/LOC2", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).createOrUpdateLocation(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC2")), any())
      }
    }

    "accept patches to a location object" in new LocationsTestScope {

      val body = HttpEntity(contentType = `application/json`, string =
        s"""
           |{
           |    "id": "LOC1",
           |    "address": "Otherstreet 12"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateLocation(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), any())
      }
    }

    "refuse patches to a location object with malformed JSON" in new LocationsTestScope {

      val body = HttpEntity(contentType = `application/json`, string =
        s"""
           | {
           |   "id": {
           |     "rant": "Hoi dit is geen ID maar gewoon een lulverhaal over dat we vaak dingen implementeren waarvan
           |              we denken dat het de specificatie zijn maar die eigenlijk zeg maar ons eigen idee zijn van hoe
           |              we zouden hopen dat het zou moeten werken maar wat dus nooit zo is omdat specificaties worden
           |              geschreven door mensen die (A) gelukkig slimmer zijn dan jij en (B) helaas geen enkel benul
           |              hebben van de problemen waar wij zo bij onze dagelijkse werkzaamheden tegenaan lopen. Thanks
           |              for listening."
           |   }
           | }
           |""".stripMargin.filterNot(_ == '\n'))

      Patch("/NL/TNM/LOC1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        handled must beFalse
        rejection must beLike { case MalformedRequestContentRejection(msg, _) if msg.contains("LocationId must be a string") => ok }
      }
    }

    "accept patches to an EVSE object" in new LocationsTestScope {

      val body = HttpEntity(contentType = `application/json`, string =
        s"""
           |{
           |    "uid": "NL-TNM-02000000",
           |    "status": "CHARGING"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1/NL-TNM-02000000", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateEvse(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), any())
      }
    }

    "accept patches to a connector object" in new LocationsTestScope {

      val body = HttpEntity(contentType = `application/json`, string =
        s"""
           |{
           |    "id": "1",
           |    "status": "CHARGING"
           |}
           |""".stripMargin)

      Patch("/NL/TNM/LOC1/NL-TNM-02000000/1", body) ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).updateConnector(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), ===(ConnectorId("1")), any())
      }
    }

    "retrieve a location object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).location(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")))
      }
    }

    "retrieve a EVSE object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).evse(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")))
      }
    }

    "retrieve a connector object" in new LocationsTestScope {
      Get("/NL/TNM/LOC1/NL-TNM-02000000/1") ~> locationsRoute.routeWithoutRh(apiUser) ~> check {
        there was one(mspLocService).connector(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), ===(ConnectorId("1")))
      }
    }

    "disallow access by authenticated but unauthorized parties" in new LocationsTestScope {
      val body = HttpEntity(contentType = `application/json`, string = "{}")

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
    val mspLocService = mock[MspLocationsService]

    mspLocService.createOrUpdateLocation(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC2")), any()) returns Future(Right(true))
    mspLocService.updateLocation(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), any()) returns Future(Right(()))
    mspLocService.location(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1"))) returns Future(Left(LocationNotFound()))
    mspLocService.evse(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000"))) returns Future(Left(LocationNotFound()))
    mspLocService.connector(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), ===(ConnectorId("1"))) returns Future(Left(LocationNotFound()))
    mspLocService.updateEvse(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), any()) returns Future(Right(()))
    mspLocService.updateConnector(===(GlobalPartyId("NL", "TNM")), ===(LocationId("LOC1")), ===(EvseUid("NL-TNM-02000000")), ===(ConnectorId("1")), any()) returns Future(Right(()))

    val apiUser = GlobalPartyId("NL", "TNM")

    val locationsRoute = new MspLocationsRoute(mspLocService)

    val loc1String = s"""
                       |{
                       |    "id": "LOC1",
                       |    "last_updated": "2014-06-25T00:00:00Z",
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
                       |        "last_updated": "2014-06-25T00:00:00Z",
                       |        "id": "ICEEVE000123_1",
                       |        "status": "AVAILABLE",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
                       |            "last_updated": "2014-06-25T00:00:00Z",
                       |            "status": "AVAILABLE",
                       |            "standard": "IEC_62196_T2",
                       |            "format": "CABLE",
                       |            "power_type": "AC_3_PHASE",
                       |            "voltage": 220,
                       |            "amperage": 16,
                       |            "tariff_id": "11"
                       |        }, {
                       |            "id": "2",
                       |            "last_updated": "2014-06-25T00:00:00Z",
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
                       |        "last_updated": "2014-06-25T00:00:00Z",
                       |        "id": "ICEEVE000123_2",
                       |        "status": "RESERVED",
                       |        "status_schedule": [],
                       |        "capabilities": [
                       |            "RESERVABLE"
                       |        ],
                       |        "connectors": [{
                       |            "id": "1",
                       |            "last_updated": "2014-06-25T00:00:00Z",
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
