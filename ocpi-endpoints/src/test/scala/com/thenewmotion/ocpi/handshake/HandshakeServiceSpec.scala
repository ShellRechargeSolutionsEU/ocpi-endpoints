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
        tokenToConnectToUs, credsToConnectToThem, ourVersionsUrlStr)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        tokenToConnectToUs, credsToConnectToThem, ourVersionsUrlStr)

      result must be_-\/.await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        tokenToConnectToUs, credsToConnectToThem, ourVersionsUrlStr)

      result must be_-\/.await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.reactToHandshakeRequest(selectedVersion,
        tokenToConnectToUs, credsToConnectToThem, ourVersionsUrlStr)

      result must be_-\/.await
    }

    "return credentials with new token if the initiating party's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
      }.await
    }

    "return error when not mutual version found" in new HandshakeTestScope{
      _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns
        Future.successful(\/-(VersionsResp(1000, None, dateTime1, List(Version("1.9", theirVersionDetailsUrl)))))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/.await
    }

    "return an error when any of the calls made to the other party endpoints don't respond" in new HandshakeTestScope{
      _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToUs) returns
        Future.successful(-\/(VersionsRetrievalFailed))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/.await
    }

    "return an error when failing in the storage of the other party endpoints" in new HandshakeTestScope{
      val handshakeServiceError = new HandshakeService {
        override def client = _client
        override def persistTheirPrefs(version: String, tokenToConnectToUs: String, credsToConnectToThem: Creds) = \/-(Unit)
        override def persistNewTokenToConnectToUs(oldToken: String, newToken: String) = -\/(CouldNotPersistNewToken)
        override def ourPartyName: String = serverPartyName
        override def ourLogo: Option[Url] = None
        override def ourWebsite: Option[Url] = None
        override def persistTheirEndpoint(version: String, tokenToConnectToUs: String, tokenToConnectToThem: String, name: String, url: Url) = \/-(Unit)
        override def ourVersionsUrl = \/-(ourVersionsUrlStr)
      }

      val result = handshakeServiceError.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/.await
    }

    "return an error when it fails sending the credentials" in new HandshakeTestScope{
      _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns
        Future.successful(-\/(SendingCredentialsFailed))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/.await
    }
  }

  class HandshakeTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    val ourVersionsUrlStr = Uri("http://localhost:8080/cpo/versions")

    val selectedVersion = "2.0"
    val tokenToConnectToUs = "123"
    val tokenToConnectToThem = "456"
    val theirVersionsUrl = "http://the-awesomes/msp/versions"
    val theirVersionDetailsUrl = "http://the-awesomes/msp/2.0"
    val credsToConnectToThem = Creds(
      tokenToConnectToThem,
      theirVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      )
    )
    val serverPartyName = "TNM (CPO)"

    val serverCredentials = Creds("123", ourVersionsUrlStr.toString(), BusinessDetails(serverPartyName, None, None))

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    var _client = mock[HandshakeClient]

    //react to handshake request
    _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", theirVersionDetailsUrl)))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
          Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
          Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"))))))

    //initiate handshake request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns Future.successful(\/-(VersionsResp(1000, None, dateTime1,
      List(Version("2.0", theirVersionDetailsUrl)))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToUs) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"))))))

    _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns Future.successful(\/-(serverCredentials))


    val handshakeService = new HandshakeService {
      override def client = _client
      override def persistTheirPrefs(version: String, tokenToConnectToUs: String, credsToConnectToThem: Creds) = \/-(Unit)
      override def persistNewTokenToConnectToUs(oldToken: String, newToken: String) = \/-(Unit)
      override def ourPartyName: String = serverPartyName
      override def ourLogo: Option[Url] = None
      override def ourWebsite: Option[Url] = None
      override def persistTheirEndpoint(version: String, tokenToConnectToUs: String, tokenToConnectToThem: String, name: String, url: Url) = \/-(Unit)
      override def ourVersionsUrl = \/-(ourVersionsUrlStr)
    }

  }
}
