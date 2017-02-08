package com.thenewmotion.ocpi.example

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.handshake.HandshakeService
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions.Endpoint
import com.thenewmotion.ocpi.msgs.v2_1.Versions.EndpointIdentifier._
import scala.concurrent.Future

class ExampleHandshakeService(implicit http: HttpExt) extends HandshakeService(
  ourNamespace = "example",
  ourPartyName = "Example",
  ourLogo = None,
  ourWebsite = None,
  ourBaseUrl = Uri("www.ocpi-example.com"),
  ourPartyId = "exp",
  ourCountryCode = "NL"
) {
  override protected def persistHandshakeReactResult(
    version: Version,
    existingTokenToConnectToUs: String,
    newTokenToConnectToUs: String,
    credsToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]) = ???

  override protected def persistUpdateCredsResult(
    version: Version,
    existingTokenToConnectToUs: String,
    newTokenToConnectToUs: String,
    credsToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]) = ???

  override protected def persistHandshakeInitResult(
    version: Version,
    newTokenToConnectToUs: String,
    newCredToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]) = ???

  override protected def persistPartyPendingRegistration(
    partyName: String,
    countryCode: String,
    partyId: String,
    newTokenToConnectToUs: String) = ???

  override protected def removePartyPendingRegistration(tokenToConnectToUs: String) = ???

  override def credsToConnectToUs(tokenToConnectToUs: String) = ???
}

object ExampleApp extends App with TopLevelRoute {
  implicit val system = ActorSystem()
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val http = Http()

  val service = new ExampleHandshakeService

  override def routingConfig = OcpiRoutingConfig(
    namespace = "example",
    versions = Map("2.1" -> OcpiVersionConfig(Map(Locations -> Left("http://locations.ocpi-example.com")))),
    handshakeService = service
  ) {
    apiUser => Future.successful(Some(ApiUser("nl", "abc")))
  } {
    internalUser => Future.successful(None)
  }

  Http().bindAndHandle(topLevelRoute, "localhost", 8080)

}
