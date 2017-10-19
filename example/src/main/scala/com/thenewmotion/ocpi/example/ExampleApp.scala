package com.thenewmotion.ocpi
package example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.VersionsRoute.OcpiVersionConfig
import com.thenewmotion.ocpi.common.TokenAuthenticator
import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import com.thenewmotion.ocpi.msgs.Versions.EndpointIdentifier._
import com.thenewmotion.ocpi.msgs.Versions.{Endpoint, VersionNumber}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.registration.{RegistrationClient, RegistrationRepo, RegistrationRoute, RegistrationService}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.VersionsJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.CredentialsJsonProtocol._
import scala.concurrent.{ExecutionContext, Future}

class ExampleRegistrationRepo extends RegistrationRepo {
  def isPartyRegistered(globalPartyId: GlobalPartyId)(implicit ec: ExecutionContext) = ???
  def findTheirAuthToken(globalPartyId: GlobalPartyId)(implicit ec: ExecutionContext) = ???
  def persistInfoAfterConnectToUs(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs],
    credsToConnectToThem: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  )(implicit ec: ExecutionContext) = ???

  def persistInfoAfterConnectToThem(
    version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs],
    newCredToConnectToThem: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  )(implicit ec: ExecutionContext) = ???
  def deletePartyInformation(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[Unit] = ???
}


object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()

  val repo = new ExampleRegistrationRepo()

  val client = new RegistrationClient()

  val service = new RegistrationService(
    client,
    repo,
    ourGlobalPartyId = GlobalPartyId("nl", "exp"),
    ourPartyName = "Example",
    ourVersions = Set(VersionNumber.`2.1`),
    ourVersionsUrl = Url("www.ocpi-example.com/ocpi/versions"))

  val registrationRoute = new RegistrationRoute(service)

  val versionRoute = new VersionsRoute(
    Future(Map(VersionNumber.`2.1` -> OcpiVersionConfig(
      Map(
        Credentials -> Right(registrationRoute.route),
        Locations -> Left(Url("http://locations.ocpi-example.com"))
      )
    )))
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
