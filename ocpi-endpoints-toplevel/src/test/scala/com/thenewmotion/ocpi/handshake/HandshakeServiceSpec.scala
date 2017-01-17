package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{BusinessDetails, Url}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import org.joda.time.DateTime
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import org.specs2.concurrent.ExecutionEnv
import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber._

class HandshakeServiceSpec(implicit ee: ExecutionEnv)  extends Specification with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "HandshakeService" should {

    "when requesting react to handshake" >> {
      "return credentials with new token if the initiating party's endpoints returned correct data" >> new HandshakeTestScope {
        val result = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

        result must beLike[\/[HandshakeError, Creds]] {
          case \/-(Creds(_, v, bd, id, c)) =>
            v mustEqual "http://localhost:8080/cpo/versions"
            bd mustEqual BusinessDetails("TNM (CPO)", None, None)
            id mustEqual ourPartyIdVal
            c mustEqual ourCountryCodeVal
        }.await
      }
      "return an error if storing the other party's endpoints failed" >> new HandshakeTestScope {
        val reactResult = handshakeServicePersistError.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)
        reactResult must beLike[\/[HandshakeError, Creds]] {
          case -\/(CouldNotPersistNewToken(t)) =>
            t mustNotEqual tokenToConnectToUs
        }.await
      }
    }
    "when requesting the initiation of the handshake" >> {
      "return credentials with new token party provided, if the connected party endpoints returned correct data" >> new HandshakeTestScope {
        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, tokenToConnectToUs, theirVersionsUrl)

        result must beLike[\/[HandshakeError, Creds]] {
          case \/-(Creds(_, v, bd, id, c)) =>
            v mustEqual "http://localhost:8080/cpo/versions"
            bd mustEqual BusinessDetails("TNM (CPO)", None, None)
            id mustEqual ourPartyIdVal
            c mustEqual ourCountryCodeVal
        }.await
      }
      "return error when no mutual version found" >> new HandshakeTestScope {
        _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns
          Future.successful(\/-(List(Version(`2.0`, theirVersionDetailsUrl))))

        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, tokenToConnectToUs, theirVersionsUrl)

        result must be_-\/(CouldNotFindMutualVersion: HandshakeError).await
      }
      "return an error when failing in the storage of the other party endpoints" >> new HandshakeTestScope {
        handshakeServicePersistError.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url) must
          be_-\/(CouldNotPersistNewToken(tokenToConnectToUs): HandshakeError).await
      }
      "return an error when it fails sending the credentials" >> new HandshakeTestScope{
        _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns
          Future.successful(-\/(SendingCredentialsFailed))

        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, tokenToConnectToUs, theirVersionsUrl)

        result must be_-\/(SendingCredentialsFailed: HandshakeError).await
      }
    }
    "when requesting the update of the credentials" >> {
      "return an error if requested for a party did not registered yet" >> new HandshakeTestScope {
        handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem) must
          be_-\/(WaitingForRegistrationRequest: HandshakeError).await
      }
      "return an error when failing in the storage of the other party endpoints" >> new HandshakeTestScope {
        handshakeServiceUpdateError.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem) must
          be_-\/(CouldNotUpdateEndpoints: HandshakeError).await
      }
    }
    "when requesting react, initiate or update handshake" >> {
      "return error if there was an error getting versions" >> new HandshakeTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(-\/(VersionsRetrievalFailed))
        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

        reactResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
        initResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
        updateResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
      }
      "return error if no versions were returned" >> new HandshakeTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(\/-(Nil))
        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

        reactResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): HandshakeError).await
        initResult must be_-\/(CouldNotFindMutualVersion: HandshakeError).await
        updateResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): HandshakeError).await
      }
      "return error if there was an error getting version details" >> new HandshakeTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
          -\/(VersionDetailsRetrievalFailed))

        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)

        reactResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
        initResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
        updateResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
      }
      "return an error if any of the required endpoints is not detailed" >> new HandshakeTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns
          Future.failed(new IllegalArgumentException)

        handshakeService.reactToHandshakeRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem) must
          throwA[IllegalArgumentException].await
        handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, credsToConnectToThem.countryCode,
          credsToConnectToThem.partyId, credsToConnectToThem.token, credsToConnectToThem.url)
          throwA[IllegalArgumentException].await
        handshakeService.reactToUpdateCredsRequest(selectedVersion, tokenToConnectToUs, credsToConnectToThem)
          throwA[IllegalArgumentException].await
      }
    }
  }

  class HandshakeTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    implicit val materializer = ActorMaterializer()

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val ourVersionsUrlStr = Uri("http://localhost:8080/cpo/versions")
    val ourBaseUrlStr = Uri("http://localhost:8080")
    val tokenToConnectToUs = "123"
    val ourCpoName = "TNM (CPO)"
    val ourPartyIdVal = "TNM"
    val ourCountryCodeVal = "NL"
    val ourCredentials = Creds(tokenToConnectToUs, ourVersionsUrlStr.toString(),
      BusinessDetails(ourCpoName, None, None), ourPartyIdVal, ourCountryCodeVal)
    val ourCredsResp = ourCredentials

    val selectedVersion = `2.1`
    val tokenToConnectToThem = "456"
    val theirVersionsUrl = "http://the-awesomes/msp/versions"
    val theirVersionDetailsUrl = "http://the-awesomes/msp/2.1"
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
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    // Initiate handshake request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToUs) returns Future.successful(
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToUs) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    _client.sendCredentials(any[Url], any[String], any[Creds])(any[ExecutionContext]) returns Future.successful(
      \/-(ourCredsResp))

    // handshakeServices
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

      protected def persistPartyPendingRegistration(
        partyName: String,
        countryCode: String,
        partyId: String,
        newTokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      protected def persistHandshakeReactResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      protected def persistUpdateCredsResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): HandshakeError \/ Unit = -\/(WaitingForRegistrationRequest)

      protected def persistHandshakeInitResult(
        version: VersionNumber,
        newTokenToConnectToUs: String,
        newCredToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      protected def removePartyPendingRegistration(
        tokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      def credsToConnectToUs(t: String) = -\/(UnknownPartyToken(tokenToConnectToUs))
    }

    val handshakeServicePersistError = new HandshakeService(
      ourNamespace = "cpo",
      ourPartyName = ourCpoName,
      ourLogo = None,
      ourWebsite = None,
      ourBaseUrl = ourBaseUrlStr,
      ourPartyId = ourPartyIdVal,
      ourCountryCode = ourCountryCodeVal
    ) {
      override val client = _client

      protected def persistPartyPendingRegistration(
        partyName: String,
        countryCode: String,
        partyId: String,
        newTokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      protected def persistHandshakeReactResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(CouldNotPersistNewToken(newTokenToConnectToUs))

      protected def persistUpdateCredsResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(WaitingForRegistrationRequest)

      protected def persistHandshakeInitResult(
        version: VersionNumber,
        tokenToConnectToUs: String,
        newCredToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(CouldNotPersistNewToken(newCredToConnectToThem.token))

      protected def removePartyPendingRegistration(
        tokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      def credsToConnectToUs(t: String) = -\/(UnknownPartyToken(tokenToConnectToUs))
    }

    val handshakeServiceUpdateError = new HandshakeService(
      ourNamespace = "cpo",
      ourPartyName = ourCpoName,
      ourLogo = None,
      ourWebsite = None,
      ourBaseUrl = ourBaseUrlStr,
      ourPartyId = ourPartyIdVal,
      ourCountryCode = ourCountryCodeVal
    ) {
      override val client = _client

      protected def persistPartyPendingRegistration(
        partyName: String,
        countryCode: String,
        partyId: String,
        newTokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      protected def persistHandshakeReactResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(CouldNotPersistNewToken(newTokenToConnectToUs))

      protected def persistUpdateCredsResult(
        version: VersionNumber,
        existingTokenToConnectToUs: String,
        newTokenToConnectToUs: String,
        credsToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(CouldNotUpdateEndpoints)

      protected def persistHandshakeInitResult(
        version: VersionNumber,
        tokenToConnectToUs: String,
        newCredToConnectToThem: Creds,
        endpoints: Iterable[Endpoint]
      ) = -\/(CouldNotPersistNewToken(newCredToConnectToThem.token))

      protected def removePartyPendingRegistration(
        tokenToConnectToUs: String
      ): HandshakeError \/ Unit = \/-(())

      def credsToConnectToUs(t: String) = -\/(UnknownPartyToken(tokenToConnectToUs))
    }
  }
}
