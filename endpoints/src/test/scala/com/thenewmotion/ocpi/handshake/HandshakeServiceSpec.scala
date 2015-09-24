package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{Url, BusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.Uri
import scala.concurrent.Future
import scalaz._

class HandshakeServiceSpec extends Specification  with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "HandshakeService should" should {
    "return credentials with new token if the initiating partie's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds, uri)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      client.getVersions(clientCreds.url, currentAuth) returns Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds, uri)

      result must be_-\/.await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      client.getVersions(clientCreds.url, currentAuth) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds, uri)

      result must be_-\/.await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      client.getVersionDetails(clientVersionDetailsUrl, currentAuth) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.startHandshake(selectedVersion,
        currentAuth, clientCreds, uri)

      result must be_-\/.await
    }

  }

  trait HandshakeTestScope extends Scope {

    val uri = Uri("http://ocpi.thenewmotion.com/monkeys")

    val selectedVersion = "2.0"
    val currentAuth = "123"
    val newAuth = "456"
    val clientVersionsUrl = "http://the-awesomes/msp/versions"
    val clientVersionDetailsUrl = "http://the-awesomes/msp/2.0"
    val clientCreds = Creds(
      newAuth,
      clientVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      )
    )

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    var client = mock[HandshakeClient]

    client.getVersions(clientCreds.url, currentAuth) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", clientVersionDetailsUrl)))))
    client.getVersionDetails(clientVersionDetailsUrl, currentAuth) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",
        List(Endpoint(EndpointIdentifier.Credentials, clientVersionDetailsUrl + "/credentials"))))))
    val handshakeService = new HandshakeService(client) {
      override def persistClientPrefs(version: String, auth: String, creds: Creds):
        Disjunction[CouldNotPersistPreferences, Unit] = \/-(Unit)

      override def persistNewToken(auth: String, newToken: String): Disjunction[CouldNotPersistNewToken, Unit] =
        \/-(Unit)

      override def partyname: String = "TNM (CPO)"

      override def persistEndpoint(version: String, auth: String, name: String, url: Url):
      Disjunction[CouldNotPersistEndpoint, Unit] = \/-(Unit)
    }

  }
}
