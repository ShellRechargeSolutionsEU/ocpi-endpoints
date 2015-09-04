package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.MediaTypes._
import spray.http.{HttpCharsets, ContentType, HttpEntity}
import spray.testkit.Specs2RouteTest

import scalaz._
import Scalaz._

class CredentialsRouteSpec extends Specification with Specs2RouteTest with Mockito {

  import spray.httpx.SprayJsonSupport._

  "credentials endpoint" should {
    "accept client credentials" in new CredentialsTestScope {

      val data =
        s"""
           |{
           |    "token": "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
           |    "url": "https://example.com/ocpi/cpo/",
           |    "business_details": {
           |        "name": "Example Operator",
           |        "logo": "http://example.com/images/logo.png",
           |        "website": "http://example.com"
           |    }
           |}
           |""".stripMargin

      val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string = data)

      Post("/credentials", body) ~> credentialsRoutes.credentialsRoute ~> check {
        handled must beTrue
      }
    }
  }

  trait CredentialsTestScope extends Scope {
    import com.thenewmotion.ocpi.credentials._

    val formatter = ISODateTimeFormat.dateTimeNoMillis().withZoneUTC
    val dateTime1 = formatter.parseDateTime("2010-01-01T00:00:00Z")

    val credentialsRoutes = new CredentialsRoutes {
      override val currentTime = mock[CurrentTime]
      currentTime.instance returns dateTime1

      val cdh: CredentialsDataHandler = new CredentialsDataHandler {
        def registerParty(creds: Credentials): CreateError \/ Unit =  \/-(Unit)

        def retrieveCredentials: ListError \/ Credentials = \/-(Credentials("ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
          "https://example.com/ocpi/cpo/", BusinessDetails("Example Operator")))

      }

      def actorRefFactory = system
    }

    val credentials1 = Creds(
      token = "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
      url = "https://example.com/ocpi/cpo/",
      business_details = OcpiBusinessDetails(
        "Example Operator",
        "http://example.com/images/logo.png",
        "http://example.com"
      )
    )
  }
}
