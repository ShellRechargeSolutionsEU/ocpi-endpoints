package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._
import msgs.Versions.EndpointIdentifier.Credentials
import msgs.Versions.{Version, VersionDetails, VersionNumber}
import msgs.v2_1.CommonTypes._
import msgs.v2_1.Credentials.Creds
import msgs.Ownership.{Ours, Theirs}
import msgs._
import RegistrationError._

class RegistrationService(
  repo: RegistrationRepo,
  ourGlobalPartyId: GlobalPartyId,
  ourPartyName: String,
  ourVersions: Set[VersionNumber],
  ourVersionsUrl: Uri,
  ourWebsite: Option[Uri] = None,
  ourLogo: Option[Image] = None
)(implicit http: HttpExt) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  private[registration] val client: RegistrationClient = new RegistrationClient

  private def errIfRegistered(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Result[RegistrationError, Unit] =
    result {
      repo.isPartyRegistered(globalPartyId).map {
        case true =>
          logger.debug("{} is already registered", globalPartyId)
          -\/(AlreadyExistingParty(globalPartyId))
        case false => \/-(())
      }
    }

  private def errIfNotRegistered(
    globalPartyId: GlobalPartyId,
    error: GlobalPartyId => RegistrationError = WaitingForRegistrationRequest
  )(implicit ec: ExecutionContext): Result[RegistrationError, Unit] =
    result {
      repo.isPartyRegistered(globalPartyId).map {
        case true => \/-(())
        case false =>
          logger.debug("{} is not registered yet", globalPartyId)
          -\/(error(globalPartyId))
      }
    }

  private def errIfNotSupported(version: VersionNumber): Result[RegistrationError, Unit] =
    result {
      if (ourVersions.contains(version)) \/-(())
      else -\/(SelectedVersionNotHostedByUs(version))
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
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Ours]] = {
    logger.info("Registration initiated by {}, for {} creds: {}", globalPartyId, version, creds)

    (for {
      _ <- errIfNotSupported(version)
      _ <- errIfRegistered(globalPartyId)
      details <- getVersionDetails(version, creds.token, Uri(creds.url))
      newTokenToConnectToUs = AuthToken.generateTheirs
      _ <- result(repo.persistInfoAfterConnectToUs(globalPartyId, version, newTokenToConnectToUs, creds, details.endpoints).map(_.right))
    } yield generateCreds(newTokenToConnectToUs)).run.map {
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
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Ours]] = {
    logger.info("Update credentials request sent by {}, for {}, creds {}", creds.globalPartyId, version, creds)

    def errIfGlobalPartyIdChangedAndTaken: Result[RegistrationError, Unit] =
      // If they try and change the global party id, make sure it's not already taken
      if (creds.globalPartyId != globalPartyId) errIfRegistered(creds.globalPartyId)
      else result(\/-(()))

    (for {
      _ <- errIfNotSupported(version)
      _ <- errIfNotRegistered(globalPartyId)
      _ <- errIfGlobalPartyIdChangedAndTaken
      details <- getVersionDetails(version, creds.token, Uri(creds.url))
      theirNewToken = AuthToken.generateTheirs
      _ <- result(repo.persistInfoAfterConnectToUs(globalPartyId, version, theirNewToken, creds, details.endpoints).map(_.right))
    } yield generateCreds(theirNewToken)).run.map {
      _.leftMap {
        e => logger.error("error during reactToUpdateCredsRequest: {}", e); e
      }
    }
  }

  private def getVersionDetails(
    version: VersionNumber,
    token: AuthToken[Ours],
    versionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Result[RegistrationError, VersionDetails] = {
    def selectedVersionInfo(versionResp: List[Version]): RegistrationError \/ Version = {
      logger.debug(s"looking for $version, versionResp: $versionResp")
      versionResp
        .find(_.version == version)
        .toRightDisjunction(SelectedVersionNotHostedByThem(version))
    }

    for {
      theirVers <- result(client.getTheirVersions(versionsUrl, token))
      commonVer <- result(selectedVersionInfo(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(commonVer.url, token))
    } yield theirVerDetails
  }

  def reactToDeleteCredsRequest(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[RegistrationError \/ Unit] = {
    logger.info("delete credentials request sent by {}", globalPartyId)

    (for {
      _ <- errIfNotRegistered(globalPartyId, CouldNotUnregisterParty)
      _ <- result(repo.deletePartyInformation(globalPartyId).map(_.right))
    } yield ()).run.map {
      _.leftMap {
        e => logger.error("error during reactToDeleteCredsRequest: {}", e); e
      }
    }
  }

  def initiateRegistrationProcess(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: ActorMaterializer) = {
    // see https://github.com/typesafehub/scalalogging/issues/16
    logger.info("initiate registration process with {}, {}", theirVersionsUrl: Any, ourToken: Any)
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.sendCredentials, errIfRegistered)
  }

  def updateRegistrationInfo(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: ActorMaterializer)= {
    logger.info("update credentials process with {}, {}", theirVersionsUrl: Any, ourToken: Any)
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.updateCredentials, errIfNotRegistered(_, WaitingForRegistrationRequest))
  }

  private def handshake(
    ourToken: AuthToken[Ours],
    theirNewToken: AuthToken[Theirs],
    theirVersionsUrl: Uri,
    credentialExchange: (Url, AuthToken[Ours], Creds[Ours]) => Future[RegistrationError \/ Creds[Theirs]],
    registrationCheck: GlobalPartyId => Result[RegistrationError, Unit]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]]= {

    def getCredsEndpoint(verDet: VersionDetails) = Future.successful(
      verDet.endpoints.find(_.identifier == Credentials) \/> {
        logger.debug("Credentials endpoint not found in retrieved endpoints for version {}", verDet.version)
        SendingCredentialsFailed: RegistrationError
      }
    )

      (for {
        details <- getLatestCommonVersionDetails(ourToken, theirVersionsUrl)
        credEp <- result(getCredsEndpoint(details))
        theirCreds <- result {
          logger.debug(s"issuing new token for party with initial authorization token: '$theirNewToken'")
          credentialExchange(credEp.url, ourToken, generateCreds(theirNewToken))
        }
        _ <- registrationCheck(theirCreds.globalPartyId)
        _ <- result(repo.persistInfoAfterConnectToThem(details.version, theirNewToken, theirCreds, details.endpoints).map(_.right)
      )
    } yield theirCreds).run.map {
        _.leftMap {
          e => logger.error("error during handshake: {}", e); e
        }
      }
  }

  private def getLatestCommonVersionDetails(
    token: AuthToken[Ours],
    theirVersionsUrl: Uri
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Result[RegistrationError, VersionDetails] = {
    def findLatestCommon(response: Iterable[Version]): RegistrationError \/ Version = {
      logger.debug(s"looking for the latest common version, versionResp: $response")

      val theirNumbers = response.map(_.version)
      val common = theirNumbers.toSet.intersect(ourVersions)
      if (common.nonEmpty) {
        response
          .find(_.version == common.max)
          .getOrElse(throw new RuntimeException("this can't be"))
          .right
      } else -\/(CouldNotFindMutualVersion)
    }

    for {
      theirVers <- result(client.getTheirVersions(theirVersionsUrl, token))
      commonVer <- result(findLatestCommon(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(commonVer.url, token))
    } yield theirVerDetails
  }

  def credsToConnectToUs(globalPartyId: GlobalPartyId)(implicit ec: ExecutionContext): Future[RegistrationError \/ Creds[Ours]] =
    (for {
      theirToken <- result(repo.findTheirAuthToken(globalPartyId).map(_ \/> UnknownParty(globalPartyId)))
    } yield generateCreds(theirToken)).run

  private def generateCreds(token: AuthToken[Theirs]): Creds[Ours] = {
    import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
    Creds[Ours](token, ourVersionsUrl.toString,
      BusinessDetails(ourPartyName, ourLogo, ourWebsite.map(_.toString)), ourGlobalPartyId)
  }
}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  protected def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)
  protected def result[L, T](value: L \/ T): Result[L, T] = EitherT(Future.successful(value))
}
