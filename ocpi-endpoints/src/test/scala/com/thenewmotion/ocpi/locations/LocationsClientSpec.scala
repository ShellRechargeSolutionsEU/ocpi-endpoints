package com.thenewmotion.ocpi.locations

import akka.actor.ActorSystem
import com.thenewmotion.ocpi.common.UnknownLinkFormat
import com.thenewmotion.ocpi.locations.LocationsError.LocationNotFound
import com.thenewmotion.ocpi.msgs.v2_0.Locations.LocationsResp
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http._
import scala.concurrent.Future
import scalaz.{-\/, \/, \/-}

class LocationsClientSpec extends Specification with FutureMatchers{

  "extractNextUri()" should {
    "parse out next uri from link header" in new LocationsTestScope {
      locClient.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; rel="next"""") mustEqual
        Some(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50"))

      val mtch = locClient.extractNextUri("""<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel="first",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel="last",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15>; rel="next",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13>; rel="prev"""")
      mtch mustEqual Some(Uri("""https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15"""))

      locClient.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; rel="last"""") mustEqual
        None
    }

    "crash on wrongly formatted link headers" in new LocationsTestScope{
      locClient.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; relation="next"""") must
        throwA[UnknownLinkFormat]
    }
  }

  "setPageLimit()" should {
    "set page limit to min(server limit, client limit)" in new LocationsTestScope {
      locClient.setPageLimit(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=150")) mustEqual
        Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=100")
      locClient.setPageLimit(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50")) mustEqual
        Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50")
    }
  }

  "locations client" should {

    "download all paginated data" in new LocationsTestScope {

      override val loc1response = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, loc1String.getBytes),
        List(linkHeader(s"$locationsUrl?offset=1&limit=1"),
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      override val loc2response = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, loc2String.getBytes),
        List(RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      locClient.getLocations(locationsUrl, "auth") must beLike[\/[LocationsError, LocationsResp]]{
        case \/-(r) =>
          r.data.size === 2
          r.data.head.id === "LOC1"
          r.data.tail.head.id === "LOC2"
      }.await
    }

    "return errors for wrong urls" in new LocationsTestScope {
      locClient.getLocations("http://localhost", "auth") must beLike[\/[LocationsError, LocationsResp]] {
        case -\/(r) =>
          r must haveClass[LocationNotFound]
      }.await
    }
  }

  trait LocationsTestScope extends Scope {

    implicit val system = ActorSystem()

    val locationsUrl = "http://localhost:8095/cpo/versions/2.0/locations"

    def linkHeader(next: Uri) =
      RawHeader("Link", s"""<$next>; rel="next"""")


    val loc1String = s"""
                       |{
                       |  "status_code": 1000,
                       |  "timestamp": "2010-01-01T00:00:00Z",
                       |  "data": [{
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
                       |  }]
                       |}
                       |""".stripMargin

    val loc2String = loc1String.replace("LOC1","LOC2")

    def loc1response: HttpResponse = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`,
      loc1String.getBytes), List(linkHeader(s"$locationsUrl?offset=1&limit=1"),
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1")
      )
    )
    def loc2response: HttpResponse = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`,
      loc2String.getBytes), List(RawHeader("X-Total-Count", "2"), RawHeader("X-Limit", "1")
      )
    )

    lazy val locClient = new LocationsClient{

      val urlPattern = s"$locationsUrl\\?offset=([0-9]+)&limit=[0-9]+".r

      override def sendAndReceive = (req:HttpRequest) => req.uri.toString match {
        case urlPattern(offset) if offset == "0" => Future.successful(loc1response)
        case urlPattern(offset) if offset == "1" => Future.successful(loc2response)
        case urlPattern(offset) => println(s"got offset $offset. "); throw new RuntimeException()
        case x => println(s"got request url |$x|. "); throw new RuntimeException()
      }
    }

  }
}
