package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.thenewmotion.ocpi.handshake.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{Url, BusinessDetails}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.specs2.matcher.{Matchers, DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.Uri
import scala.concurrent.{ExecutionContext, Future}
import scalaz._

class HandshakeServiceSpec extends Specification  with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "HandshakeService" should {
    "return credentials with new token if the initiating party's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      _client.getVersionDetails(clientVersionDetailsUrl, clientCreds.token) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        currentClientAuth, clientCreds, serverVersionsUrl)

      result must be_-\/.await
    }

    "return credentials with new token if the initiating party's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.initiateHandshakeProcess(currentClientAuth, clientVersionsUrl)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error when not mutual version found" in new HandshakeTestScope{
      _client.getVersions(clientVersionsUrl, currentClientAuth) returns
        Future.successful(\/-(VersionsResp(1000, None, dateTime1, List(Version("1.9", clientVersionDetailsUrl)))))

      val result = handshakeService.initiateHandshakeProcess(currentClientAuth, clientVersionsUrl)

      result must be_-\/.await
    }

    "return an error when any of the calls made to the other party endpoints don't respond" in new HandshakeTestScope{
      _client.getVersionDetails(clientVersionDetailsUrl, currentClientAuth) returns
        Future.successful(-\/(VersionsRetrievalFailed))

      val result = handshakeService.initiateHandshakeProcess(currentClientAuth, clientVersionsUrl)

      result must be_-\/.await
    }

    "return an error when failing in the storage of the other party endpoints" in new HandshakeTestScope{
      val handshakeServiceError = new HandshakeService {
        override def client = _client
        def persistClientPrefs(version: String, auth: String, creds: Creds) = \/-(Unit)
        def persistNewToken(auth: String, newToken: String) = -\/(CouldNotPersistNewToken())
        def partyname: String = serverPartyName
        def logo: Option[Url] = None
        def website: Option[Url] = None
        def persistEndpoint(version: String, auth: String, name: String, url: Url) = \/-(Unit)
        def getHostedVersionsUrl = \/-(serverVersionsUrl)
      }

      val result = handshakeServiceError.initiateHandshakeProcess(currentClientAuth, clientVersionsUrl)

      result must be_-\/.await
    }

    "return an error when it fails sending the credentials" in new HandshakeTestScope{
      _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns
        Future.successful(-\/(SendingCredentialsFailed))

      val result = handshakeService.initiateHandshakeProcess(currentClientAuth, clientVersionsUrl)

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
    val serverPartyName = "TNM (CPO)"

    val serverCredentials = Creds("123", serverVersionsUrl.toString(), BusinessDetails(serverPartyName, None, None))

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    var _client = mock[HandshakeClient]

    //react to handshake request
    _client.getVersions(clientCreds.url, clientCreds.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", clientVersionDetailsUrl)))))

    _client.getVersionDetails(clientVersionDetailsUrl, clientCreds.token) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
          Endpoint(EndpointIdentifier.Credentials, clientVersionDetailsUrl + "/credentials"),
          Endpoint(EndpointIdentifier.Locations, clientVersionDetailsUrl + "/locations"))))))

    //initiate handshake request
    _client.getVersions(clientVersionsUrl, currentClientAuth) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", clientVersionDetailsUrl)))))

    _client.getVersionDetails(clientVersionDetailsUrl, currentClientAuth) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
        Endpoint(EndpointIdentifier.Credentials, clientVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, clientVersionDetailsUrl + "/locations"))))))

    _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns Future.successful(\/-(serverCredentials))


    val handshakeService = new HandshakeService {
      override def client = _client
      override def persistClientPrefs(version: String, auth: String, creds: Creds) = \/-(Unit)
      override def persistNewToken(auth: String, newToken: String)= \/-(Unit)
      override def partyname: String = serverPartyName
      override def logo: Option[Url] = None
      override def website: Option[Url] = None
      override def persistEndpoint(version: String, auth_for_server_api: String, auth_for_client_api: String, name: String, url: Url) = \/-(Unit)
      override def getHostedVersionsUrl = \/-(serverVersionsUrl)
    }

  }
}
