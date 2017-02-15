package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.testkit.TestKit
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, OurAuthToken, TheirAuthToken}
import com.thenewmotion.ocpi.msgs.Versions._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.Versions.EndpointIdentifier.Versions
import org.joda.time.DateTime
import org.specs2.matcher.{DisjunctionMatchers, FutureMatchers}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.{ExecutionContext, Future}
import scalaz._
import org.specs2.concurrent.ExecutionEnv
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber._
import com.thenewmotion.ocpi.msgs._

class HandshakeServiceSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "HandshakeService" should {

    "when requesting react to handshake" >> {
      "return credentials with new token if the initiating party's endpoints returned correct data" >> new HandshakeTestScope {
        val result = handshakeService.reactToHandshakeRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        result must beLike[\/[HandshakeError, Creds[TheirAuthToken]]] {
          case \/-(Creds(_, v, bd, id, c)) =>
            v mustEqual "http://ocpi.newmotion.com/cpo/versions"
            bd mustEqual BusinessDetails("TNM (CPO)", None, None)
            id mustEqual ourPartyId
            c mustEqual ourCountryCode
        }.await
      }
    }
    "when requesting the initiation of the handshake" >> {
      "return credentials with new token party provided, if the connected party endpoints returned correct data" >> new HandshakeTestScope {
        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must beLike[\/[HandshakeError, Creds[OurAuthToken]]] {
          case \/-(Creds(_, v, bd, id, c)) =>
            v mustEqual "http://the-awesomes/msp/versions"
            bd mustEqual BusinessDetails("The Awesomes", None, None)
            id mustEqual theirPartyId
            c mustEqual theirCountryCode
        }.await
      }
      "return error when no mutual version found" >> new HandshakeTestScope {
        _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns
          Future.successful(\/-(List(Version(`2.0`, theirVersionDetailsUrl))))

        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must be_-\/(CouldNotFindMutualVersion: HandshakeError).await
      }
      "return an error when it fails sending the credentials" >> new HandshakeTestScope{
        _client.sendCredentials(any[Url], any[OurAuthToken], any[Creds[TheirAuthToken]])(any[ExecutionContext], any[ActorMaterializer]) returns
          Future.successful(-\/(SendingCredentialsFailed))

        val result = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must be_-\/(SendingCredentialsFailed: HandshakeError).await
      }
    }
    "when requesting the update of the credentials" >> {
      "return an error if requested for a party did not registered yet" >> new HandshakeTestScope {
        handshakeService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem) must
          be_-\/(WaitingForRegistrationRequest: HandshakeError).await
      }
    }
    "when requesting react, initiate or update handshake" >> {
      "return error if there was an error getting versions" >> new HandshakeTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(-\/(VersionsRetrievalFailed))
        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
        initResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
        updateResult must be_-\/(VersionsRetrievalFailed: HandshakeError).await
      }
      "return error if no versions were returned" >> new HandshakeTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(\/-(Nil))
        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name, theirGlobalId,
          credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): HandshakeError).await
        initResult must be_-\/(CouldNotFindMutualVersion: HandshakeError).await
        updateResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): HandshakeError).await
      }
      "return error if there was an error getting version details" >> new HandshakeTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
          -\/(VersionDetailsRetrievalFailed))

        val reactResult = handshakeService.reactToHandshakeRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = handshakeService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
        initResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
        updateResult must be_-\/(VersionDetailsRetrievalFailed: HandshakeError).await
      }
      "return an error if any of the required endpoints is not detailed" >> new HandshakeTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns
          Future.failed(new IllegalArgumentException)

        handshakeService.reactToHandshakeRequest(selectedVersion, theirGlobalId, credsToConnectToThem) must
          throwA[IllegalArgumentException].await
        handshakeService.initiateHandshakeProcess(credsToConnectToThem.businessDetails.name,
          GlobalPartyId(credsToConnectToThem.countryCode, credsToConnectToThem.partyId),
          credsToConnectToThem.token, credsToConnectToThem.url)
          throwA[IllegalArgumentException].await
        handshakeService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
          throwA[IllegalArgumentException].await
      }
    }
  }

  class HandshakeTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    implicit val materializer = ActorMaterializer()

    implicit val http = Http()

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val ourVersionsUrlStr = Uri("http://ocpi.newmotion.com/cpo/versions")
    val ourBaseUrlStr = Uri("http://ocpi.newmotion.com")
    val tokenToConnectToUs = TheirAuthToken("123")
    val ourCpoName = "TNM (CPO)"
    val ourPartyId = PartyId("TNM")
    val ourCountryCode = CountryCode("NL")
    val ourCredentials = Creds(tokenToConnectToUs, ourVersionsUrlStr.toString(),
      BusinessDetails(ourCpoName, None, None), ourPartyId, ourCountryCode)
    val ourCredsResp = ourCredentials

    val selectedVersion = `2.1`
    val tokenToConnectToThem = OurAuthToken("456")
    val theirVersionsUrl = "http://the-awesomes/msp/versions"
    val theirVersionDetailsUrl = "http://the-awesomes/msp/2.1"
    val theirCountryCode = CountryCode("DE")
    val theirPartyId = PartyId("TAW")
    val theirGlobalId = GlobalPartyId(theirCountryCode, theirPartyId)

    val credsToConnectToThem = Creds(
      tokenToConnectToThem,
      theirVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      ),
      theirPartyId,
      theirCountryCode
    )

    var _client = mock[HandshakeClient]

    // React to handshake request
    _client.getTheirVersions(credsToConnectToThem.url, tokenToConnectToThem) returns Future.successful(
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    // Initiate handshake request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    _client.sendCredentials(any[Url], any[OurAuthToken], any[Creds[TheirAuthToken]])(any[ExecutionContext], any[ActorMaterializer]) returns Future.successful(
      \/-(credsToConnectToThem))

    // handshakeServices
    val handshakeService = new HandshakeService(
      ourNamespace = "cpo",
      ourPartyName = ourCpoName,
      ourLogo = None,
      ourWebsite = None,
      ourBaseUrl = ourBaseUrlStr,
      ourPartyId = ourPartyId,
      ourCountryCode = ourCountryCode
    ) {
      override val client = _client

      protected def persistPartyPendingRegistration(
        partyName: String,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: TheirAuthToken
      ): HandshakeError \/ Unit = \/-(())

      protected def persistHandshakeReactResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: TheirAuthToken,
        credsToConnectToThem: Creds[OurAuthToken],
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      protected def persistUpdateCredsResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: TheirAuthToken,
        credsToConnectToThem: Creds[OurAuthToken],
        endpoints: Iterable[Endpoint]
      ): HandshakeError \/ Unit = -\/(WaitingForRegistrationRequest)

      protected def persistHandshakeInitResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: TheirAuthToken,
        newCredToConnectToThem: Creds[OurAuthToken],
        endpoints: Iterable[Endpoint]
      ): Disjunction[HandshakeError, Unit] = \/-(())

      protected def removePartyPendingRegistration(
        globalPartyId: GlobalPartyId
      ): HandshakeError \/ Unit = \/-(())

      override def ourVersionsUrl = ourBaseUrlStr + "/" + "cpo" + "/" + Versions.name

      override protected def getTheirAuthToken(globalPartyId: GlobalPartyId) = ???
    }
  }
}
