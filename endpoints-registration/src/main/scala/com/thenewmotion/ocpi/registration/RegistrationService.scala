package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import cats.syntax.either._
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.Versions.EndpointIdentifier.Credentials
import com.thenewmotion.ocpi.msgs.Versions.{Version, VersionDetails, VersionNumber}
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.registration.RegistrationError._
import scala.concurrent.ExecutionContext

/**
  * Requires IO to call the other side during registration.
  */
class RegistrationService(
  client: RegistrationClient,
  repo: RegistrationRepo[IO],
  ourGlobalPartyId: GlobalPartyId,
  ourPartyName: String,
  ourVersions: Set[VersionNumber],
  ourVersionsUrl: Url,
  ourWebsite: Option[Url] = None,
  ourLogo: Option[Image] = None
)(
  implicit cs: ContextShift[IO]
) extends IOEitherUtils {

  private val logger = Logger(getClass)
  private def redact(s: String) = s.take(3) + "**REDACTED**"

  private def errIfRegistered(
    globalPartyId: GlobalPartyId
  ): Result[RegistrationError, Unit] =
    result {
      repo.isPartyRegistered(globalPartyId).map {
        case true =>
          logger.debug("{} is already registered", globalPartyId)
          AlreadyExistingParty(globalPartyId).asLeft
        case false => ().asRight
      }
    }

  private def errIfNotRegistered(
    globalPartyId: GlobalPartyId,
    error: GlobalPartyId => RegistrationError = WaitingForRegistrationRequest
  ): Result[RegistrationError, Unit] =
    result {
      repo.isPartyRegistered(globalPartyId).map {
        case true => ().asRight
        case false =>
          logger.debug("{} is not registered yet", globalPartyId)
          error(globalPartyId).asLeft
      }
    }

  private def errIfNotSupported(version: VersionNumber): Result[RegistrationError, Unit] =
    result {
      if (ourVersions.contains(version)) ().asRight
      else SelectedVersionNotHostedByUs(version).asLeft
    }

  /**
    * React to a post credentials request.
    *
    * @return new credentials to connect to us
    */
  def reactToNewCredsRequest(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    creds: Creds[Theirs]
  )(implicit ec: ExecutionContext, mat: Materializer): IO[Either[RegistrationError, Creds[Ours]]] = {
    logger.info("Registration initiated by {}, for {}", globalPartyId: Any, version: Any)

    (for {
      _ <- errIfNotSupported(version)
      _ <- errIfRegistered(globalPartyId)
      details <- getTheirVersionDetails(version, creds.token, creds.url)
      newTokenToConnectToUs = AuthToken.generateTheirs
      _ <- result(
        repo.persistInfoAfterConnectToUs(globalPartyId, version, newTokenToConnectToUs,
          creds, details.endpoints).map(_.asRight[RegistrationError])
      )
    } yield generateCreds(newTokenToConnectToUs)).value.map {
      _.leftMap {
        e => logger.error("error during reactToPostCredsRequest: {}", e); e
      }
    }
  }

  /**
    * React to a update credentials request.
    *
    * @return new credentials to connect to us
    */
  def reactToUpdateCredsRequest(
    globalPartyId: GlobalPartyId,
    version: VersionNumber,
    creds: Creds[Theirs]
  )(implicit ec: ExecutionContext, mat: Materializer): IO[Either[RegistrationError, Creds[Ours]]] = {
    logger.info("Update credentials request sent by {}, for {}", creds.globalPartyId: Any, version: Any)

    def errIfGlobalPartyIdChangedAndTaken: Result[RegistrationError, Unit] =
      // If they try and change the global party id, make sure it's not already taken
      if (creds.globalPartyId != globalPartyId) errIfRegistered(creds.globalPartyId)
      else result(().asRight)

    (for {
      _ <- errIfNotSupported(version)
      _ <- errIfNotRegistered(globalPartyId)
      _ <- errIfGlobalPartyIdChangedAndTaken
      details <- getTheirVersionDetails(version, creds.token, creds.url)
      theirNewToken = AuthToken.generateTheirs
      _ <- result(
        repo.persistInfoAfterConnectToUs(
          globalPartyId, version,
           theirNewToken, creds, details.endpoints
        ).map(_.asRight[RegistrationError]))
    } yield generateCreds(theirNewToken)).value.map {
      _.leftMap {
        e => logger.error("error during reactToUpdateCredsRequest: {}", e); e
      }
    }
  }

  private def getTheirVersionDetails(
    version: VersionNumber,
    token: AuthToken[Ours],
    versionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: Materializer): Result[RegistrationError, VersionDetails] = {
    def selectedVersionInfo(versionResp: List[Version]): Either[RegistrationError, Version] = {
      logger.debug(s"looking for $version, versionResp: $versionResp")
      versionResp
        .find(_.version == version)
        .toRight(SelectedVersionNotHostedByThem(version))
    }

    for {
      theirVers <- result(client.getTheirVersions(versionsUrl, token))
      commonVer <- result(selectedVersionInfo(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(commonVer.url, token))
    } yield theirVerDetails
  }

  def reactToDeleteCredsRequest(
    globalPartyId: GlobalPartyId
  ): IO[Either[RegistrationError, Unit]] = {
    logger.info("delete credentials request sent by {}", globalPartyId)

    (for {
      _ <- errIfNotRegistered(globalPartyId, CouldNotUnregisterParty)
      _ <- result(repo.deletePartyInformation(globalPartyId).map(_.asRight[RegistrationError]))
    } yield ()).value.map {
      _.leftMap {
        e => logger.error("error during reactToDeleteCredsRequest: {}", e); e
      }
    }
  }

  def initiateRegistrationProcess(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: Materializer): IO[Either[RegistrationError, Creds[Theirs]]] = {
    // see https://github.com/typesafehub/scalalogging/issues/16
    logger.info("initiate registration process with {}, {}", theirVersionsUrl: Any, redact(ourToken.value))
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.sendCredentials, errIfRegistered)
  }

  def updateRegistrationInfo(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: Materializer): IO[Either[RegistrationError, Creds[Theirs]]] = {
    logger.info("update credentials process with {}, {}", theirVersionsUrl: Any, redact(ourToken.value))
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.updateCredentials, errIfNotRegistered(_, WaitingForRegistrationRequest))
  }

  private def handshake(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri,
    credentialExchange: (Url, AuthToken[Ours], Creds[Ours]) => IO[Either[RegistrationError, Creds[Theirs]]],
    registrationCheck: GlobalPartyId => Result[RegistrationError, Unit]
  )(implicit ec: ExecutionContext, mat: Materializer): IO[Either[RegistrationError, Creds[Theirs]]] = {

    def getCredsEndpoint(verDet: VersionDetails) = IO.pure(
      verDet.endpoints.find(_.identifier == Credentials) toRight {
        logger.debug("Credentials endpoint not found in retrieved endpoints for version {}", verDet.version)
        SendingCredentialsFailed: RegistrationError
      }
    )

    (for {
      details <- getLatestCommonVersionDetails(ourToken, theirVersionsUrl)
      credEp <- result(getCredsEndpoint(details))
      theirCreds <- result {
        logger.debug(s"issuing new token for party with initial authorization token: '${redact(theirNewToken.value)}'")
        credentialExchange(credEp.url, ourToken, generateCreds(theirNewToken))
      }
      _ <- registrationCheck(theirCreds.globalPartyId)
      _ <- result{
        logger.debug(s"registration successful; agreed version: ${details.version}; theirCreds: **REDACTED**")
        repo.persistInfoAfterConnectToThem(
          details.version,
          theirNewToken,
          theirCreds,
          details.endpoints
        ).map(_.asRight[RegistrationError])
      }
    } yield theirCreds).value.map {
        _.leftMap {
          e => logger.error("error during handshake: {}", e); e
        }
      }
  }

  private def getLatestCommonVersionDetails(
    token: AuthToken[Ours],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: Materializer): Result[RegistrationError, VersionDetails] = {
    def findLatestCommon(response: Iterable[Version]): Either[RegistrationError, Version] = {
      logger.debug(s"looking for the latest common version, versionResp: $response")

      val theirNumbers = response.map(_.version)
      val common = theirNumbers.toSet.intersect(ourVersions)
      if (common.nonEmpty) {
        response
          .find(_.version == common.max)
          .getOrElse(throw new RuntimeException("this can't be"))
          .asRight
      } else CouldNotFindMutualVersion.asLeft
    }

    for {
      theirVers <- result(client.getTheirVersions(theirVersionsUrl, token))
      commonVer <- result(findLatestCommon(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(commonVer.url, token))
    } yield theirVerDetails
  }

  def credsToConnectToUs(globalPartyId: GlobalPartyId): IO[Either[RegistrationError, Creds[Ours]]] =
    (for {
      theirToken <- result(repo.findTheirAuthToken(globalPartyId).map(_ toRight UnknownParty(globalPartyId)))
    } yield generateCreds(theirToken)).value

  private def generateCreds(token: AuthToken[Theirs]): Creds[Ours] = {
    import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
    Creds[Ours](token, ourVersionsUrl,
      BusinessDetails(ourPartyName, ourLogo, ourWebsite), ourGlobalPartyId)
  }
}

trait IOEitherUtils {
  type Result[E, T] = EitherT[IO, E, T]

  protected def result[L, T](effect: IO[Either[L, T]]): Result[L, T] = EitherT(effect)
  protected def result[L, T](value: Either[L, T]): Result[L, T] = EitherT(IO.pure(value))
}
