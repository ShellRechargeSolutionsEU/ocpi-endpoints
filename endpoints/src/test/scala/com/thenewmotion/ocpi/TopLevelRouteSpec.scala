package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.handshake._
import com.thenewmotion.ocpi.locations.LocationsDataHandler
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails, Url}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions.VersionsResp
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, MissingHeaderRejection}
import spray.testkit.Specs2RouteTest

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz._



class TopLevelRouteSpec extends Specification with Specs2RouteTest with Mockito{

   "api" should {
     "extract the token value from the header value" in {
       val auth = new Authenticator(adh = mock[AuthDataHandler])
       auth.extractTokenValue("Basic 12345") must beNone
       auth.extractTokenValue("Token 12345") mustEqual Some("12345")
       auth.extractTokenValue("Token ") must beNone
     }

     "authenticate api calls with valid auth info" in new TopLevelScope {
       Get("/cpo/versions") ~>
         addHeader(authTokenHeader) ~> topLevelRoute.allRoutes ~> check {
         handled must beTrue
       }
     }

     "reject api calls without valid auth header" in new TopLevelScope {
       Get("/cpo/versions") ~>
         addHeader(invalidAuthTokenHeader) ~> topLevelRoute.allRoutes ~> check {
         handled must beFalse
         rejections must contain(MissingHeaderRejection("Authorization"))
       }
     }

     "reject api calls without valid auth token" in new TopLevelScope {
       Get("/cpo/versions") ~>
         addHeader(invalidAuthToken) ~> topLevelRoute.allRoutes ~> check {
         handled must beFalse
         rejections must contain(AuthenticationFailedRejection(CredentialsRejected,List()))
       }
     }

     // ----------------------------------------------------------------

     "route calls to versions endpoint" in new RoutingScope {
       Get("/cpo/versions") ~>
         addHeader(authTokenHeader) ~> topLevelRoute.allRoutes ~> check {
         handled must beTrue
         there was one(_vdh).allVersions
       }
     }

     "route calls to version details" in new RoutingScope {
       Get("/cpo/2.0") ~>
         addHeader(authTokenHeader) ~> topLevelRoute.allRoutes ~> check {
         handled must beTrue
         there was one(_vdh).versionDetails(any)
       }
     }

      "route calls to version details when terminated by slash" in new RoutingScope {
        Get("/cpo/2.0/") ~>
          addHeader(authTokenHeader) ~> topLevelRoute.allRoutes ~> check {
          handled must beTrue
          there was one(_vdh).versionDetails(any)
        }
      }

     "route calls to credentials endpoint" in new RoutingScope {
       import spray.http.MediaTypes._
       import spray.http._
       val body = HttpEntity(contentType = ContentType(`application/json`, HttpCharsets.`UTF-8`), string =
         """
           |{
           |    "token": "ebf3b399-779f-4497-9b9d-ac6ad3cc44d2",
           |    "url": "https://example.com/ocpi/cpo/",
           |    "business_details": {
           |        "name": "Example Operator",
           |        "logo": "http://example.com/images/logo.png",
           |        "website": "http://example.com"
           |    }
           |}
         """.stripMargin)

       Post("/cpo/2.0/credentials", body) ~>
         addHeader(authTokenHeader) ~> topLevelRoute.allRoutes ~> check {
         handled must beTrue
         there was one(_hss).registerVersionsEndpoint(any, any, any)
       }
     }
   }

   trait TopLevelScope extends Scope{
     import com.thenewmotion.ocpi.versions._

     val authTokenHeader = RawHeader("Authorization", "Token 12345")
     val invalidAuthTokenHeader = RawHeader("Auth", "Token 12345")
     val invalidAuthToken = RawHeader("Authorization", "Token letmein")

     val topLevelRoute = new TopLevelRoutes {
       val client = mock[HandshakeClient]
       val creds1 = Creds("", "", OcpiBusinessDetails("", None, None))
       val checks = List()
       override val handshakeService = mock[HandshakeService]
       handshakeService.registerVersionsEndpoint(any, any, any) returns \/-(creds1)

       val hdh = new HandshakeDataHandler {
         def persistClientPrefs(version: String, auth: String, creds: Credentials) = ???

         def persistNewToken(auth: String, newToken: String) = ???

         def config: HandshakeConfig = HandshakeConfig("",0,"","","","")

         def persistEndpoint(version: String, auth: String, name: String, url: Url) = ???
       }
       val vdh = new VersionsDataHandler {
         def allVersions = Map("2.0" -> "http://hardcoded.com/cpo/2.0/").right
         def versionDetails(version: Version) = -\/(UnknownVersion)
       }
       val tldh = new TopLevelRouteDataHandler {
         def namespace: String = "cpo"
       }
       val adh = new AuthDataHandler {
         def authenticateApiUser(token: String) = if (token == "12345") Some(ApiUser("beCharged","12345")) else None
       }
       val ldh = new LocationsDataHandler {
         def endpoint = "locations"
       }
       def actorRefFactory = system
     }


   }

  trait RoutingScope extends Scope {
    import com.thenewmotion.ocpi.versions._

    val authTokenHeader = RawHeader("Authorization", "Token 12345")

    val _cdh = mock[HandshakeDataHandler]
    _cdh.config returns HandshakeConfig("",0,"","","","credentials")
    _cdh.persistClientPrefs(any, any, any) returns \/-(Unit)

    val _vdh = mock[VersionsDataHandler]
    _vdh.versionsPath returns "versions"
    _vdh.allVersions returns Map("2.0" -> "http://hardcoded.com/cpo/2.0/").right
    _vdh.versionDetails(any) returns List().right
    val creds1 = Creds("", "", OcpiBusinessDetails("", None, None))
    val _hss = mock[HandshakeService]
    _hss.registerVersionsEndpoint(any, any, any) returns \/-(creds1)

    val topLevelRoute = new TopLevelRoutes {
      val checks = List()
      override val handshakeService = _hss
      val client = mock[HandshakeClient]
      client.getVersions(any, any) returns Future(\/-(VersionsResp(1000, None, DateTime.now(),List())))
      val hdh = _cdh
      val vdh = _vdh
      val tldh = new TopLevelRouteDataHandler {
        def namespace: String = "cpo"
      }
      val adh = new AuthDataHandler {
        def authenticateApiUser(token: String) = if (token == "12345") Some(ApiUser("beCharged","12345")) else None
      }
      val ldh = new LocationsDataHandler {
        def endpoint = "locations"
      }

      def actorRefFactory = system
    }

  }
 }
