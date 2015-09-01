package com.thenewmotion.ocpi


import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.testkit.Specs2RouteTest

import scalaz.Scalaz._
import scalaz._

class VersionsRouteSpec extends Specification with Specs2RouteTest with Mockito{

  import OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.GenericSuccess._
  import com.thenewmotion.ocpi.msgs.v2_0.Versions._
  import spray.httpx.SprayJsonSupport._

  "versions endpoint" should {
    "return all available versions" in new VersionsRouteScope {
      Get("/versions") ~> versionsRoute.versionsRoute ~> check {
        responseAs[VersionsResp] === VersionsResp(code, default_message, dateTime1,
          List(Version("2.0", "http://hardcoded.com/cpo/2.0/")))
      }
    }
  }
  "endpoint for a specific version" should {
    "return all endpoints for chosen version" in new VersionsRouteScope {
      Get("/2.0") ~> versionsRoute.versionsRoute ~> check {
        responseAs[VersionDetailsResp] === VersionDetailsResp(code, default_message,
          dateTime1, VersionDetails("2.0", List( Endpoint(EndpointIdentifierEnum.Credentials,
            "http://hardcoded.com/cpo/2.0/credentials"))))
      }
    }
    "reject unknown versions" in new VersionsRouteScope {
      Get("/2.1") ~> versionsRoute.versionsRoute ~> check {
        rejections must contain(UnknownVersionRejection("2.1"))
      }
    }
  }

  trait VersionsRouteScope extends Scope {
    import com.thenewmotion.ocpi.versions._

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val dateTime1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

    val versionsRoute = new VersionsRoutes {
      override val currentTime = mock[CurrentTime]
      currentTime.instance returns dateTime1
      val vdh = new VersionsDataHandler {
        def allVersions = Map("2.0" -> "http://hardcoded.com/cpo/2.0/").right
        def versionDetails(version: Version): \/[ListError, List[Endpoint]] =
          if (version == "2.0")  List(Endpoint(EndpointTypeEnum.Credentials, "2.0", "http://hardcoded.com/cpo/2" +
            ".0/credentials")).right
          else UnknownVersion.left
      }
      def actorRefFactory = system
    }
  }
}
