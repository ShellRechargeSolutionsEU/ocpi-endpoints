package com.thenewmotion.ocpi
package example

import _root_.akka.actor.ActorSystem
import _root_.akka.http.scaladsl.Http
import _root_.akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import _root_.akka.http.scaladsl.server.Directives._
import _root_.akka.stream.ActorMaterializer
import cats.effect.{ContextShift, IO}
import com.thenewmotion.ocpi.VersionsRoute.OcpiVersionConfig
import com.thenewmotion.ocpi.common.TokenAuthenticator
import com.thenewmotion.ocpi.msgs.Ownership.Theirs
import com.thenewmotion.ocpi.msgs.Versions.EndpointIdentifier._
import com.thenewmotion.ocpi.msgs.Versions.{Endpoint, VersionNumber}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.registration.{RegistrationClient, RegistrationRepo, RegistrationRoute, RegistrationService}


class ExampleRegistrationRepo extends RegistrationRepo[IO] {
  def isPartyRegistered(globalPartyId: GlobalPartyId): IO[Boolean] = ???
  def findTheirAuthToken(globalPartyId: GlobalPartyId): IO[Option[AuthToken[Theirs]]] = ???
  def persistInfoAfterConnectToUs(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs],
    credsToConnectToThem: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): IO[Unit] = ???

  def persistInfoAfterConnectToThem(
    version: VersionNumber,
    newTokenToConnectToUs: AuthToken[Theirs],
    newCredToConnectToThem: Creds[Theirs],
    endpoints: Iterable[Endpoint]
  ): IO[Unit] = ???

  def deletePartyInformation(
    globalPartyId: GlobalPartyId
  ): IO[Unit] = ???
}


object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()
  implicit private val ctxShift: ContextShift[IO] = IO.contextShift(executor)

  val repo = new ExampleRegistrationRepo()

  val client = new RegistrationClient[IO]()

  val service = new RegistrationService(
    client,
    repo,
    ourGlobalPartyId = GlobalPartyId("nl", "exp"),
    ourPartyName = "Example",
    ourVersions = Set(VersionNumber.`2.1`),
    ourVersionsUrl = Url("www.ocpi-example.com/ocpi/versions"))

  import com.thenewmotion.ocpi.common.HktMarshallableInstances._
  val registrationRoute = RegistrationRoute[IO](service)

  val versionRoute = VersionsRoute(
    IO.pure(Map(VersionNumber.`2.1` -> OcpiVersionConfig(
      Map(
        Credentials -> Right(registrationRoute.apply),
        Locations -> Left(Url("http://locations.ocpi-example.com"))
      )
    )))
  )

  val auth = new TokenAuthenticator(_ => IO.pure(Some(GlobalPartyId("NL", "TNM"))))

  val topLevelRoute = {
    pathPrefix("example") {
      authenticateOrRejectWithChallenge(auth) { user =>
        pathPrefix("versions") {
          versionRoute(user)
        }
      }
    }
  }

  Http().bindAndHandle(topLevelRoute, "localhost", 8080)

}
