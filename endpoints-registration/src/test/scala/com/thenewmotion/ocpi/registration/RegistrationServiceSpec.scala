package com.thenewmotion.ocpi
package registration

import java.time.ZonedDateTime
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.testkit.TestKit
import cats.effect.{ContextShift, IO}
import com.thenewmotion.ocpi.common.IOMatchersExt
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.Versions.EndpointIdentifier.Versions
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber._
import com.thenewmotion.ocpi.msgs.Versions._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId, _}
import com.thenewmotion.ocpi.registration.RegistrationError._
import org.mockito.ArgumentMatchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.EitherMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.ExecutionContext

class RegistrationServiceSpec(implicit ee: ExecutionEnv) extends Specification with Mockito with IOMatchersExt
  with EitherMatchers {

  implicit val s: ContextShift[IO] = IO.contextShift(ee.executionContext)

  "RegistrationService" should {

    "when requesting react to registration" >> {
      "return credentials with new token if the initiating party's endpoints returned correct data" >> new RegistrationTestScope {
        repo.isPartyRegistered(ArgumentMatchers.eq(theirGlobalId)) returns IO.pure(false)
        repo.persistInfoAfterConnectToUs(any(), any(), any(), any(), any()) returns IO.pure(())

        val result = registrationService.reactToNewCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        result must returnValueLike[Either[RegistrationError, Creds[Ours]]] {
          case Right(Creds(_, v, bd, gpi)) =>
            v mustEqual Url("http://ocpi.newmotion.com/cpo/versions")
            bd mustEqual BusinessDetails("TNM (CPO)", None, None)
            gpi mustEqual ourGlobalPartyId
        }
      }

      "return error if there was an error getting versions" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Left(VersionsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val reactResult = registrationService.reactToNewCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        reactResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(VersionsRetrievalFailed) => ok
        }
      }
      "return error if no versions were returned" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Right(Nil))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val reactResult = registrationService.reactToNewCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        reactResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(SelectedVersionNotHostedByThem(`selectedVersion`)) => ok
        }
      }
      "return an error if the selected version is not supported by us" >> new RegistrationTestScope {
        val result = registrationService.reactToNewCredsRequest(theirGlobalId, `2.0`, credsToConnectToThem)
        result must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(SelectedVersionNotHostedByUs(`2.0`)) => ok
        }
      }
      "return an error if the selected version is not supported by them" >> new RegistrationTestScope {
        _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns
          IO.pure(Right(List(Version(`2.0`, theirVersionDetailsUrl))))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val result = registrationService.reactToNewCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)
        result must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(SelectedVersionNotHostedByThem(`2.1`)) => ok
        }
      }
      "return error if there was an error getting version details" >> new RegistrationTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns IO.pure(
          Left(VersionDetailsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val reactResult = registrationService.reactToNewCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        reactResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(VersionDetailsRetrievalFailed) => ok
        }
      }
    }
    "when requesting the initiation of the registration" >> {
      "return credentials with new token party provided, if the connected party endpoints returned correct data" >> new RegistrationTestScope {
        repo.isPartyRegistered(ArgumentMatchers.eq(theirGlobalId)) returns IO.pure(false)
        repo.persistInfoAfterConnectToThem(any(), any(), any(), any()) returns IO.pure(())

        val result = registrationService.initiateRegistrationProcess(tokenToConnectToThem, tokenToConnectToUs, theirVersionsUrl)

        result must returnValueLike[Either[RegistrationError, Creds[Theirs]]] {
          case Right(Creds(_, v, bd, gpi)) =>
            v mustEqual Url("http://the-awesomes/msp/versions")
            bd mustEqual BusinessDetails("The Awesomes", None, None)
            gpi mustEqual theirGlobalId
        }
      }
      "return error when no mutual version found" >> new RegistrationTestScope {

        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns
          IO.pure(Right(List(Version(`2.0`, theirVersionDetailsUrl))))

        val result = registrationService.initiateRegistrationProcess(tokenToConnectToThem, tokenToConnectToUs,
          theirVersionsUrl)

        result must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(CouldNotFindMutualVersion) => ok
        }
      }
      "return an error when it fails sending the credentials" >> new RegistrationTestScope {
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        _client.sendCredentials(Url(any[String]), any(), any())(any(), any(), any()) returns
          IO.pure(Left(SendingCredentialsFailed))

        val result = registrationService.initiateRegistrationProcess(tokenToConnectToThem, tokenToConnectToUs,
          theirVersionsUrl)

        result must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(SendingCredentialsFailed) => ok
        }
      }

      "return error if there was an error getting versions" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Left(VersionsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)
        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.token,
          tokenToConnectToUs, credsToConnectToThem.url)
        initResult must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(VersionsRetrievalFailed) => ok
        }
      }
      "return error if no versions were returned" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Right(Nil))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)
        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.token,
          tokenToConnectToUs, credsToConnectToThem.url)

        initResult must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(CouldNotFindMutualVersion) => ok
        }
      }
      "return error if there was an error getting version details" >> new RegistrationTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns IO.pure(
          Left(VersionDetailsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val initResult = registrationService.initiateRegistrationProcess(credsToConnectToThem.token,
          tokenToConnectToUs, credsToConnectToThem.url)

        initResult must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(VersionDetailsRetrievalFailed) => ok
        }
      }
    }
    "when requesting the update of the registration Information" >> {
      "return credentials with new token party provided, if the connected party endpoints returned correct data" >> new RegistrationTestScope {
        repo.isPartyRegistered(ArgumentMatchers.eq(theirGlobalId)) returns IO.pure(true)
        repo.persistInfoAfterConnectToThem(any(), any(), any(), any()) returns IO.pure(())

        val result = registrationService.updateRegistrationInfo(tokenToConnectToThem, tokenToConnectToUs, theirVersionsUrl)

        result must returnValueLike[Either[RegistrationError, Creds[Theirs]]] {
          case Right(Creds(_, v, bd, gpi)) =>
            v mustEqual Url("http://the-awesomes/msp/versions")
            bd mustEqual BusinessDetails("The Awesomes", None, None)
            gpi mustEqual theirGlobalId
        }
      }
      "return error when party is not registered" >> new RegistrationTestScope {

        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        val result = registrationService.updateRegistrationInfo(tokenToConnectToThem, tokenToConnectToUs,
          theirVersionsUrl)

        result must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
          case Left(WaitingForRegistrationRequest(`theirGlobalId`)) => ok
        }
      }

    }
    "when requesting the update of the credentials" >> {
      "return an error if requested for a party did not registered yet" >> new RegistrationTestScope {

        repo.isPartyRegistered(theirGlobalId) returns IO.pure(false)

        registrationService.reactToUpdateCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem) must
          returnValueLike[Either[RegistrationError, Creds[Ours]]]{
            case Left(WaitingForRegistrationRequest(`theirGlobalId`)) => ok
          }
      }

      "return an error if trying to set their global id to one that is already taken" >> new RegistrationTestScope {

        repo.isPartyRegistered(theirGlobalId) returns IO.pure(true)

        val newGlobalId = GlobalPartyId("DEQWE")

        repo.isPartyRegistered(newGlobalId) returns IO.pure(true)
        val newCreds = credsToConnectToThem.copy[Theirs](globalPartyId = newGlobalId)

        registrationService.reactToUpdateCredsRequest(theirGlobalId, selectedVersion, newCreds) must
          returnValueLike[Either[RegistrationError, Creds[Ours]]]{
            case Left(AlreadyExistingParty(`newGlobalId`)) => ok
          }
      }

      "return error if there was an error getting versions" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Left(VersionsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(true)

        val updateResult = registrationService.reactToUpdateCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        updateResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(VersionsRetrievalFailed) => ok
        }
      }
      "return error if no versions were returned" >> new RegistrationTestScope {
        _client.getTheirVersions(credsToConnectToThem.url, credsToConnectToThem.token) returns
          IO.pure(Right(Nil))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(true)

        val updateResult = registrationService.reactToUpdateCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        updateResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(SelectedVersionNotHostedByThem(`selectedVersion`)) => ok
        }
      }
      "return error if there was an error getting version details" >> new RegistrationTestScope {
        _client.getTheirVersionDetails(theirVersionDetailsUrl, credsToConnectToThem.token) returns IO.pure(
          Left(VersionDetailsRetrievalFailed))
        repo.isPartyRegistered(theirGlobalId) returns IO.pure(true)

        val updateResult = registrationService.reactToUpdateCredsRequest(theirGlobalId, selectedVersion, credsToConnectToThem)

        updateResult must returnValueLike[Either[RegistrationError, Creds[Ours]]]{
          case Left(VersionDetailsRetrievalFailed) => ok
        }
      }
    }
  }

  class RegistrationTestScope(_system: ActorSystem) extends TestKit(_system) with Scope {

    def this() = this(ActorSystem("ocpi-allstarts"))

    implicit val http = Http()

    val dateTime1 = ZonedDateTime.parse("2010-01-01T00:00:00Z")

    val ourVersionsUrlStr = Url("http://ocpi.newmotion.com/cpo/versions")
    val ourBaseUrlStr = Url("http://ocpi.newmotion.com")
    val tokenToConnectToUs = AuthToken[Theirs]("123")
    val ourCpoName = "TNM (CPO)"
    val ourGlobalPartyId = GlobalPartyId("NL","TNM")
    val ourCredentials = Creds[Ours](tokenToConnectToUs, ourVersionsUrlStr,
      BusinessDetails(ourCpoName, None, None), ourGlobalPartyId)
    val ourCredsResp = ourCredentials

    val selectedVersion = `2.1`
    val tokenToConnectToThem = AuthToken[Ours]("456")
    val theirVersionsUrl = Url("http://the-awesomes/msp/versions")
    val theirVersionDetailsUrl = Url("http://the-awesomes/msp/2.1")
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

    var _client = mock[RegistrationClient[IO]]

    // React to credentials request
    _client.getTheirVersions(credsToConnectToThem.url, tokenToConnectToThem) returns IO.pure(
      Right(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns IO.pure(
      Right(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl / "credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl / "locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl / "tariffs")))))

    // Initiate credentials request
    _client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem) returns IO.pure(
      Right(List(Version(`2.1`, theirVersionDetailsUrl))))

    _client.getTheirVersionDetails(theirVersionDetailsUrl, tokenToConnectToThem) returns IO.pure(
      Right(VersionDetails(`2.1`,List(
        Endpoint(EndpointIdentifier.Credentials, theirVersionDetailsUrl / "credentials"),
        Endpoint(EndpointIdentifier.Locations, theirVersionDetailsUrl / "locations"),
        Endpoint(EndpointIdentifier.Tariffs, theirVersionDetailsUrl / "tariffs")))))

    _client.sendCredentials(Url(any[String]), any[AuthToken[Ours]], any[Creds[Ours]])(any[ExecutionContext],
      any[ContextShift[IO]], any[Materializer]) returns IO.pure(Right(credsToConnectToThem))

    _client.updateCredentials(Url(any[String]), any[AuthToken[Ours]], any[Creds[Ours]])(any[ExecutionContext],
      any[ContextShift[IO]], any[Materializer]) returns IO.pure(Right(credsToConnectToThem))

    val repo = mock[RegistrationRepo[IO]]

    // registrationServices
    val registrationService = new RegistrationService(
      _client,
      repo,
      ourVersions = Set(`2.1`),
      ourVersionsUrl = ourBaseUrlStr / "cpo" / Versions.value,
      ourGlobalPartyId = ourGlobalPartyId,
      ourPartyName = ourCpoName)
  }
}
