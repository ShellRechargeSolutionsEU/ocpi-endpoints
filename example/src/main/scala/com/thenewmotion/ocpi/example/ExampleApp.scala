package com.thenewmotion.ocpi
package example

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import VersionsRoute.OcpiVersionConfig
import handshake.{HandshakeRoute, HandshakeService}
import msgs.{CountryCode, GlobalPartyId, PartyId, OurAuthToken, TheirAuthToken}
import msgs.v2_1.Credentials.Creds
import msgs.Versions.{Endpoint, VersionNumber}
import msgs.Versions.EndpointIdentifier._
import akka.http.scaladsl.server.Directives._
import common.TokenAuthenticator
import scala.concurrent.Future

class ExampleHandshakeService(implicit http: HttpExt) extends HandshakeService(
  ourNamespace = "example",
  ourPartyName = "Example",
  ourLogo = None,
  ourWebsite = None,
  ourBaseUrl = Uri("www.ocpi-example.com"),
  ourPartyId = PartyId("exp"),
  ourCountryCode = CountryCode("NL")
) {

  override protected def persistPartyPendingRegistration(partyName: String, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken) = ???

  override protected def removePartyPendingRegistration(globalPartyId: GlobalPartyId) = ???

  override protected def persistHandshakeReactResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken, credsToConnectToThem: Creds[OurAuthToken], endpoints: Iterable[Endpoint]) = ???

  override protected def persistUpdateCredsResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken, credsToConnectToThem: Creds[OurAuthToken], endpoints: Iterable[Endpoint]) = ???

  override protected def persistHandshakeInitResult(version: VersionNumber, globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken, newCredToConnectToThem: Creds[OurAuthToken], endpoints: Iterable[Endpoint]) = ???

  override def ourVersionsUrl = "http://versions.ocpi-example.com"

  override protected def getTheirAuthToken(globalPartyId: GlobalPartyId) = ???
}

object ExampleApp extends App {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()

  val service = new ExampleHandshakeService

  val handshakeRoute = new HandshakeRoute(service)

  val versionRoute = new VersionsRoute(
    Map(VersionNumber.`2.1` -> OcpiVersionConfig(
      Map(
        Credentials -> Right(handshakeRoute.route),
        Locations -> Left("http://locations.ocpi-example.com")
      )
    ))
  )

  val auth = new TokenAuthenticator(_ => Future.successful(Some(GlobalPartyId(CountryCode("NL"), PartyId("TNM")))))

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
