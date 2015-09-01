package com.thenewmotion.ocpi

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.BasicHttpCredentials
import spray.testkit.Specs2RouteTest
import scalaz.Scalaz._

class TopLevelRouteSpec extends Specification with Specs2RouteTest with Mockito{

   "api" should {
     "authenticate apiusers" in new TopLevelScope {
       Get("/cpo/versions") ~>
         addCredentials(validCredentials) ~> topLevelRoute.allRoutes ~> check {
         handled must beTrue
       }
     }
   }

   trait TopLevelScope extends Scope {
     import com.thenewmotion.ocpi.versions._

     val validCredentials = BasicHttpCredentials("beCharged", "123")
     val topLevelRoute = new TopLevelRoutes {
       val vdh = new VersionsDataHandler {
         def allVersions = Map("2.0" -> "http://hardcoded.com/cpo/2.0/").right
         def versionDetails(version: Version) = ???
       }
       val tldh = new TopLevelRouteDataHanlder {
         def namespace: String = "cpo"
       }
       val adh: AuthDataHandler = new AuthDataHandler {
         def apiuser(token: String) = if (token == "123") Some(ApiUser("beCharged")) else None
       }
       def actorRefFactory = system
     }


   }
 }
