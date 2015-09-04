package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.credentials.{Credentials, CredentialsDataHandler}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{AuthenticationFailedRejection, MissingHeaderRejection}
import spray.testkit.Specs2RouteTest

import scalaz.Scalaz._
import scalaz.\/

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
   }

   trait TopLevelScope extends Scope {
     import com.thenewmotion.ocpi.versions._

     val authTokenHeader = RawHeader("Authorization", "Token 12345")
     val invalidAuthTokenHeader = RawHeader("Auth", "Token 12345")
     val invalidAuthToken = RawHeader("Authorization", "Token letmein")
     val topLevelRoute = new TopLevelRoutes {
       val cdh = new CredentialsDataHandler {
         def registerParty(creds: Credentials): \/[CreateError, Unit] = ???

         def retrieveCredentials: \/[ListError, Credentials] = ???
       }
       val vdh = new VersionsDataHandler {
         def allVersions = Map("2.0" -> "http://hardcoded.com/cpo/2.0/").right
         def versionDetails(version: Version) = ???
       }
       val tldh = new TopLevelRouteDataHanlder {
         def namespace: String = "cpo"
       }
       val adh: AuthDataHandler = new AuthDataHandler {
         def apiuser(token: String) = if (token == "12345") Some(ApiUser("beCharged")) else None
       }
       def actorRefFactory = system
     }


   }
 }
