package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.testkit.TestKit
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
    "return credentials with new token if the initiating party's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.startHandshake(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.startHandshake(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.startHandshake(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      _client.getVersionDetails(clientVersionDetailsUrl, clientCreds.token) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.startHandshake(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

  }

  class HandshakeTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))
    
    val serverVersionsUrl = Uri("http://localhost:8080/cpo/versions")

    val selectedVersion = "2.0"
    val currentClientAuth = "123"
    val newAuthForServer = "456"
    val clientVersionsUrl = "http://the-awesomes/msp/versions"
    val clientVersionDetailsUrl = "http://the-awesomes/msp/2.0"
    val clientCreds = Creds(
      newAuthForServer,
      clientVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      )
    )

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    var _client = mock[HandshakeClient]

    _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", clientVersionDetailsUrl)))))
    _client.getVersionDetails(clientVersionDetailsUrl, clientCreds.token) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",
        List(Endpoint(EndpointIdentifier.Credentials, clientVersionDetailsUrl + "/credentials"))))))


    val handshakeService = new HandshakeService {
      override def client = _client
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
