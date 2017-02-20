package com.thenewmotion.ocpi.registration

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.testkit.TestKit
import com.thenewmotion.ocpi.registration.RegistrationError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId}
import com.thenewmotion.ocpi.msgs.Versions._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
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

class RegistrationServiceSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with FutureMatchers
  with DisjunctionMatchers{

  "RegistrationService" should {

    "when requesting react to registration" >> {
      "return credentials with new token if the initiating party's endpoints returned correct data" >> new RegistrationTestScope {
        val result = registrationService.reactToPostCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        result must beLike[\/[RegistrationError, Creds[Ours]]] {
          case \/-(Creds(_, v, bd, gpi)) =>
            v mustEqual "http://ocpi.newmotion.com/cpo/versions"
            bd mustEqual BusinessDetails("TNM (CPO)", None, None)
            gpi mustEqual ourGlobalPartyId
        }.await
      }
    }
    "when requesting the initiation of the registration" >> {
      "return credentials with new token party provided, if the connected party endpoints returned correct data" >> new RegistrationTestScope {
        val result = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must beLike[\/[RegistrationError, Creds[Theirs]]] {
          case \/-(Creds(_, v, bd, gpi)) =>
            v mustEqual "http://the-awesomes/msp/versions"
            bd mustEqual BusinessDetails("The Awesomes", None, None)
            gpi mustEqual theirGlobalId
        }.await
      }
      "return error when no mutual version found" >> new RegistrationTestScope {
        _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns
          Future.successful(\/-(List(Version(`2.0`, theirVersionDetailsUrl))))

        val result = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must be_-\/(CouldNotFindMutualVersion: RegistrationError).await
      }
      "return an error when it fails sending the credentials" >> new RegistrationTestScope{
        _client.sendCredentials(any[Url], any[AuthToken[Ours]], any[Creds[Ours]])(any[ExecutionContext], any[ActorMaterializer]) returns
          Future.successful(-\/(SendingCredentialsFailed))

        val result = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, tokenToConnectToThem, theirVersionsUrl)

        result must be_-\/(SendingCredentialsFailed: RegistrationError).await
      }
    }
    "when requesting the update of the credentials" >> {
      "return an error if requested for a party did not registered yet" >> new RegistrationTestScope {
        registrationService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem) must
          be_-\/(WaitingForRegistrationRequest: RegistrationError).await
      }
    }
    "when requesting react, initiate or update registration" >> {
      "return error if there was an error getting versions" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(-\/(VersionsRetrievalFailed))
        val reactResult = registrationService.reactToPostCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = registrationService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(VersionsRetrievalFailed: RegistrationError).await
        initResult must be_-\/(VersionsRetrievalFailed: RegistrationError).await
        updateResult must be_-\/(VersionsRetrievalFailed: RegistrationError).await
      }
      "return error if no versions were returned" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          Future.successful(\/-(Nil))
        val reactResult = registrationService.reactToPostCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name, theirGlobalId,
          credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = registrationService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): RegistrationError).await
        initResult must be_-\/(CouldNotFindMutualVersion: RegistrationError).await
        updateResult must be_-\/(SelectedVersionNotHostedByThem(selectedVersion): RegistrationError).await
      }
      "return error if there was an error getting version details" >> new RegistrationTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns Future.successful(
          -\/(VersionDetailsRetrievalFailed))

        val reactResult = registrationService.reactToPostCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          theirGlobalId, credsToConnectToThem.token, credsToConnectToThem.url)
        val updateResult = registrationService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)

        reactResult must be_-\/(VersionDetailsRetrievalFailed: RegistrationError).await
        initResult must be_-\/(VersionDetailsRetrievalFailed: RegistrationError).await
        updateResult must be_-\/(VersionDetailsRetrievalFailed: RegistrationError).await
      }
      "return an error if any of the required endpoints is not detailed" >> new RegistrationTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns
          Future.failed(new IllegalArgumentException)

        registrationService.reactToPostCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem) must
          throwA[IllegalArgumentException].await
        registrationService.initiateRegistrationProcess(credsToConnectToThem.businessDetails.name,
          credsToConnectToThem.globalPartyId, credsToConnectToThem.token, credsToConnectToThem.url)
          throwA[IllegalArgumentException].await
        registrationService.reactToUpdateCredsRequest(selectedVersion, theirGlobalId, credsToConnectToThem)
          throwA[IllegalArgumentException].await
      }
    }
  }

  class RegistrationTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    implicit val materializer = ActorMaterializer()

    implicit val http = Http()

    val dateTime1 = DateTime.parse("2010-01-01T00:00:00Z")

    val ourVersionsUrlStr = Uri("http://ocpi.newmotion.com/cpo/versions")
    val ourBaseUrlStr = Uri("http://ocpi.newmotion.com")
    val tokenToConnectToUs = AuthToken[Theirs]("123")
    val ourCpoName = "TNM (CPO)"
    val ourGlobalPartyId = GlobalPartyId("NL","TNM")
    val ourCredentials = Creds[Ours](tokenToConnectToUs, ourVersionsUrlStr.toString(),
      BusinessDetails(ourCpoName, None, None), ourGlobalPartyId)
    val ourCredsResp = ourCredentials

    val selectedVersion = `2.1`
    val tokenToConnectToThem = AuthToken[Ours]("456")
    val theirVersionsUrl = "http://the-awesomes/msp/versions"
    val theirVersionDetailsUrl = "http://the-awesomes/msp/2.1"
    val theirGlobalId = GlobalPartyId("DE", "TAW")

    val credsToConnectToThem = Creds[Theirs](
      tokenToConnectToThem,
      theirVersionsUrl,
      BusinessDetails(
        "The Awesomes",
        None,
        None
      ),
      theirGlobalId
    )

    var _client = mock[RegistrationClient]

    // React to credentials request
    _client.getTheirVersions(credsToConnectToThem.url, tokenToConnectToThem) returns Future.successful(
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    // Initiate credentials request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns Future.successful(
      \/-(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl + "/credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl + "/locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl + "/tariffs")))))

    _client.sendCredentials(any[Url], any[AuthToken[Ours]], any[Creds[Ours]])(any[ExecutionContext], any[ActorMaterializer]) returns Future.successful(
      \/-(credsToConnectToThem))

    // registrationServices
    val registrationService = new RegistrationService(
      ourPartyName = ourCpoName,
      ourLogo = None,
      ourWebsite = None,
      ourBaseUrl = ourBaseUrlStr,
      ourGlobalPartyId = ourGlobalPartyId
    ) {
      override val client = _client

      protected def persistPartyPendingRegistration(
        partyName: String,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: AuthToken[Theirs]
      ): RegistrationError \/ Unit = \/-(())

      protected def persistPostCredsResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: AuthToken[Theirs],
        credsToConnectToThem: Creds[Theirs],
        endpoints: Iterable[Endpoint]
      ): Disjunction[RegistrationError, Unit] = \/-(())

      protected def persistUpdateCredsResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: AuthToken[Theirs],
        credsToConnectToThem: Creds[Theirs],
        endpoints: Iterable[Endpoint]
      ): RegistrationError \/ Unit = -\/(WaitingForRegistrationRequest)

      protected def persistRegistrationInitResult(
        version: VersionNumber,
        globalPartyId: GlobalPartyId,
        newTokenToConnectToUs: AuthToken[Theirs],
        newCredToConnectToThem: Creds[Theirs],
        endpoints: Iterable[Endpoint]
      ): Disjunction[RegistrationError, Unit] = \/-(())

      protected def removePartyPendingRegistration(
        globalPartyId: GlobalPartyId
      ): RegistrationError \/ Unit = \/-(())

      override def ourVersionsUrl = ourBaseUrlStr + "/" + "cpo" + "/" + Versions.value

      override protected def getTheirAuthToken(globalPartyId: GlobalPartyId) = ???
    }
  }
}
