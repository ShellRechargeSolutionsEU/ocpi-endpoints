package com.thenewmotion.ocpi
package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import VersionsRoute.OcpiVersionConfig
import registration.{RegistrationRepo, RegistrationRoute, RegistrationService}
import msgs._
import msgs.v2_1.Credentials.Creds
import msgs.Versions.{Endpoint, VersionNumber}
import msgs.Versions.EndpointIdentifier._
import akka.http.scaladsl.server.Directives._
import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import common.TokenAuthenticator
import scala.concurrent.{ExecutionContext, Future}

class ExampleRegistrationRepo extends RegistrationRepo {
  override def isPartyRegistered(globalPartyId: GlobalPartyId)(implicit ec: ExecutionContext) = ???

  override def findTheirAuthToken(globalPartyId: GlobalPartyId)(implicit ec: ExecutionContext) = ???

  override def persistNewCredsResult(globalPartyId: GlobalPartyId, version: VersionNumber,
                                     newTokenToConnectToUs: AuthToken[Theirs], credsToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint])
                                    (implicit ec: ExecutionContext) = ???

  override def persistUpdateCredsResult(globalPartyId: GlobalPartyId, version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs], credsToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint])
    (implicit ec: ExecutionContext) = ???

  override def persistRegistrationInitResult(version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs], newCredToConnectToThem: Creds[Theirs], endpoints: Iterable[Endpoint])
    (implicit ec: ExecutionContext) = ???
}


object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()

  val repo = new ExampleRegistrationRepo()

  val service = new RegistrationService(repo,
    ourGlobalPartyId = GlobalPartyId("nl", "exp"),
    ourPartyName = "Example",
    ourVersionsUrl = Uri("www.ocpi-example.com/ocpi/versions"))

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
