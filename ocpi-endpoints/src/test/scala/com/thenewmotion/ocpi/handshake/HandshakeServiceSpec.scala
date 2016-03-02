package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails, Url}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds, CredsResp}
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import org.joda.time.DateTime
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
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
      val result = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd, id, c)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
          id mustEqual ourPartyIdVal
          c mustEqual ourCountryCodeVal
      }.await
    }

    "return error if there was an error getting versions" in new HandshakeTestScope {
      _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
        Future.successful(-\/(VersionsRetrievalFailed))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

      result must be_-\/(VersionsRetrievalFailed: HandshakeError).await
    }

    "return error if no versions were returned" in new HandshakeTestScope {
      _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
        Future.successful(\/-(VersionsResp(1000, None, dateTime1,
        List())))
      val result = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

      result must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): HandshakeError).await
    }

    "return error if there was an error getting version details" in new HandshakeTestScope {
      _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
        -\/(VersionDetailsRetrievalFailed))

      val result = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

      result must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
    }

    "return credentials with new token if the initiating party's endpoints returned correct data" in new HandshakeTestScope {
      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must beLike[\/[HandshakeError, Creds]] {
        case \/-(Creds(_, v, bd, id, c)) =>
          v mustEqual "http://localhost:8080/cpo/versions"
          bd mustEqual BusinessDetails("TNM (CPO)", None, None)
          id mustEqual ourPartyIdVal
          c mustEqual ourCountryCodeVal
      }.await
    }

    "return error when not mutual version found" in new HandshakeTestScope{
      _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns
        Future.successful(\/-(VersionsResp(1000, None, dateTime1, List(Version("1.9", theirVersionDetailsUrl)))))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/(CouldNotFindMutualVersion: HandshakeError).await
    }

    "return an error when any of the calls made to the other party endpoints don't respond" in new HandshakeTestScope{
      _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns
        Future.successful(-\/(VersionsRetrievalFailed))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/(VersionsRetrievalFailed: HandshakeError).await
    }

    "return an error when failing in the storage of the other party endpoints" in new HandshakeTestScope{
      val handshakeServiceError = new HandshakeService(
        ourNamespace = "cpo",
        ourPartyName = ourCpoName,
        ourLogo = None,
        ourWebsite = None,
        ourBaseUrl = ourBaseUrlStr,
        ourPartyId = ourPartyIdVal,
        ourCountryCode = ourCountryCodeVal
        ) {
        override val client = _client

        protected def persistHandshakeReactResult(
          version: String,
          existingTokenToConnectToUs: String,
          newTokenToConnectToUs: String,
          credsToConnectToThem: Creds,
          endpoints: Iterable[Endpoint]
        ): Disjunction[HandshakeError, Unit] = -\/(CouldNotPersistNewToken(newTokenToConnectToUs))

        protected def persistUpdateCredsResult(
          version: String,
          existingTokenToConnectToUs: String,
          newTokenToConnectToUs: String,
          credsToConnectToThem: Creds,
          endpoints: Iterable[Endpoint]
        ): HandshakeError \/ Unit = -\/(WaitingForRegistrationRequest)

        protected def persistHandshakeInitResult(
          version: String,
          tokenToConnectToUs: String,
          newCredToConnectToThem: Creds,
          endpoints: Iterable[Endpoint]
        ): Disjunction[HandshakeError, Unit] = -\/(CouldNotPersistNewToken(newCredToConnectToThem.token))

        def findRegisteredCredsToConnectToUs(t: String) = -\/(UnknownPartyToken(tokenToConnectToUs))
      }

      val result = handshakeServiceError.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/(CouldNotPersistNewToken(tokenToConnectToUs): HandshakeError).await
    }

    "return an error when it fails sending the credentials" in new HandshakeTestScope{
      _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns
        Future.successful(-\/(SendingCredentialsFailed))

      val result = handshakeService.initiateHandshakeProcess(tokenToConnectToUs, theirVersionsUrl)

      result must be_-\/(SendingCredentialsFailed: HandshakeError).await
    }

    "return an error if any of the required endpoints is not detailed" in new HandshakeTestScope {
      _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns
        Future.failed(new IllegalArgumentException)

      handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem) must
        throwA[IllegalArgumentException].await
    }

    "return an error if failed persisting the new credentials details" in new HandshakeTestScope {
      handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem) must
        be_-\/(WaitingForRegistrationRequest: HandshakeError).await
    }
  }

  class HandshakeTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val ourVersionsUrlStr = Uri("http://localhost:8080/cpo/versions")
    val ourBaseUrlStr = Uri("http://localhost:8080")
    val tokenToConnectToUs = "123"
    val ourCpoName = "TNM (CPO)"
    val ourPartyIdVal = "TNM"
    val ourCountryCodeVal = "NL"
    val ourCredentials = Creds(tokenToConnectToUs, ourVersionsUrlStr.toString(),
      BusinessDetails(ourCpoName, None, None), ourPartyIdVal, ourCountryCodeVal)
    val ourCredsResp = CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message), dateTime1, ourCredentials)

    val selectedVersion = "2.0"
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
      ),
      ourPartyIdVal,
      ourCountryCodeVal
    )

    var _client = mock[HandshakeClient]

    // React to handshake request
    _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns Future.successful(
      \/-(VersionsResp(1000, None, dateTime1, List(Version("2.0", theirVersionDetailsUrl)))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
          Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
          Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
          Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs"))))))

    // Initiate handshake request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns Future.successful(
      \/-(VersionsResp(1000, None, dateTime1, List(Version("2.0", theirVersionDetailsUrl)))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToUs) returns Future.successful(
      \/-(VersionDetailsResp(1000,None,dateTime1, VersionDetails("2.0",List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs"))))))

    _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns Future.successful(
      \/-(ourCredsResp))


    val handshakeService = new HandshakeService(
      ourNamespace = "cpo",
      ourPartyName = ourCpoName,
      ourLogo = None,
      ourWebsite = None,
      ourBaseUrl = ourBaseUrlStr,
      ourPartyId = ourPartyIdVal,
      ourCountryCode = ourCountryCodeVal
      ) {
      override val client = _client

      protected def persistHandshakeReactResult(
        version: String,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      protected def persistUpdateCredsResult(
        version: String,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): HandshakeError \/ Unit = -\/(WaitingForRegistrationRequest)

      protected def persistHandshakeInitResult(
        version: String,
        newTokenToConnectToUs: String,
        newCredToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      def findRegisteredCredsToConnectToUs(t: String) = -\/(UnknownPartyToken(tokenToConnectToUs))
    }
  }
}
