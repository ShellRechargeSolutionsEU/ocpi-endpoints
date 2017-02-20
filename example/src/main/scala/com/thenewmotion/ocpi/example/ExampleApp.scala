package com.thenewmotion.ocpi
package example

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import VersionsRoute.OcpiVersionConfig
import registration.{RegistrationRoute, RegistrationService}
import msgs._
import msgs.v2_1.Credentials.Creds
import msgs.Versions.{Endpoint, VersionNumber}
import msgs.Versions.EndpointIdentifier._
import akka.http.scaladsl.server.Directives._
import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import common.TokenAuthenticator
import scala.concurrent.Future

class ExampleRegistrationService(implicit http: HttpExt) extends RegistrationService(
  ourPartyName = "Example",
  ourLogo = None,
  ourWebsite = None,
  ourBaseUrl = Uri("www.ocpi-example.com"),
  ourGlobalPartyId = GlobalPartyId("nl", "exp")
) {

  override protected def persistPartyPendingRegistration(partyName: String, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs]) = ???

  override protected def removePartyPendingRegistration(globalPartyId: GlobalPartyId) = ???

  override protected def persistPostCredsResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs], credsToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint]) = ???

  override protected def persistUpdateCredsResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs], credsToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint]) = ???

  override protected def persistRegistrationInitResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs], newCredToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint]) = ???

  override def ourVersionsUrl = "http://versions.ocpi-example.com"

  override protected def getTheirAuthToken(globalPartyId: GlobalPartyId) = ???
}

object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()

  val service = new ExampleRegistrationService

  val registrationRoute = new RegistrationRoute(service)

  val versionRoute = new VersionsRoute(
    Map(VersionNumber.`2.1` -> OcpiVersionConfig(
      Map(
        Credentials -> Right(registrationRoute.route),
        Locations -> Left("http://locations.ocpi-example.com")
      )
    ))
  )

  val auth = new TokenAuthenticator(_ => Future.successful(Some(GlobalPartyId("NL", "TNM"))))

  val topLevelRoute = {
    path("example") {
      authenticateOrRejectWithChallenge(auth) { user =>
        path("versions") {
          versionRoute.route(user)
        }
      }
    }
  }

  Http().bindAndHandle(topLevelRoute, "localhost", 8080)

}
