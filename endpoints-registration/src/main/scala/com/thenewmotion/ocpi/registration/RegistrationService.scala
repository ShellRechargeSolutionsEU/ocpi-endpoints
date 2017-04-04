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
  ourVersionsUrl: Uri,
  ourWebsite: Option[Uri] = None,
  ourLogo: Option[Image] = None)(implicit http: HttpExt) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  private[registration] val client: RegistrationClient = new RegistrationClient

  private def errIfRegistered(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[RegistrationError \/ Unit] =
    repo.isPartyRegistered(globalPartyId).map {
      case true =>
        logger.debug("{} is already registered", globalPartyId)
        -\/(AlreadyExistingParty(globalPartyId))
      case false => \/-(())
    }

  private def errIfNotRegistered(
    globalPartyId: GlobalPartyId,
    error: GlobalPartyId => RegistrationError = WaitingForRegistrationRequest
  )(implicit ec: ExecutionContext): Future[RegistrationError \/ Unit] =
    repo.isPartyRegistered(globalPartyId).map {
      case true => \/-(())
      case false =>
        logger.debug("{} is not registered yet", globalPartyId)
        -\/(error(globalPartyId))
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
      _ <- result(errIfRegistered(globalPartyId))
      verDetails <- result(getTheirDetails(
        version, creds.token, Uri(creds.url), initiatedByUs = false))
      newTokenToConnectToUs = AuthToken.generateTheirs
      _ <- result(repo.persistInfoAfterConnectToUs(
        globalPartyId, version, newTokenToConnectToUs, creds, verDetails.endpoints
      ).map(_.right))
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

    def errIfGlobalPartyIdChangedAndTaken =
      // If they try and change the global party id, make sure it's not already taken
      if (creds.globalPartyId != globalPartyId) {
        errIfRegistered(creds.globalPartyId)
      } else Future.successful(\/-(()))

    (for {
      _ <- result(errIfNotRegistered(globalPartyId))
      _ <- result(errIfGlobalPartyIdChangedAndTaken)
      verDetails <- result(getTheirDetails(version, creds.token, Uri(creds.url), initiatedByUs = false))
      theirNewToken = AuthToken.generateTheirs
      _ <- result(
        repo.persistInfoAfterConnectToUs(globalPartyId, version, theirNewToken, creds, verDetails.endpoints
      ).map(_.right))
    } yield generateCreds(theirNewToken)).run.map {
      _.leftMap {
        e => logger.error("error during reactToUpdateCredsRequest: {}", e); e
      }
    }
  }

  def reactToDeleteCredsRequest(
    globalPartyId: GlobalPartyId
  )(implicit ec: ExecutionContext): Future[RegistrationError \/ Unit] = {
    logger.info("delete credentials request sent by {}", globalPartyId)

    (for {
      _ <- result(errIfNotRegistered(globalPartyId, CouldNotUnregisterParty))
      _ <- result(repo.deletePartyInformation(globalPartyId).map(_.right))
    } yield ()).run.map {
      _.leftMap {
        e => logger.error("error during reactToDeleteCredsRequest: {}", e); e
      }
    }
  }

  def initiateRegistrationProcess(ourToken: AuthToken[Ours], theirNewToken: AuthToken[Theirs], theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext, mat: ActorMaterializer) = {
    // see https://github.com/typesafehub/scalalogging/issues/16
    logger.info("initiate registration process with {}, {}", theirVersionsUrl: Any, ourToken: Any)
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.sendCredentials, errIfRegistered)
  }

  def updateRegistrationInfo(ourToken: AuthToken[Ours], theirNewToken: AuthToken[Theirs], theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext, mat: ActorMaterializer)= {
    logger.info("update credentials process with {}, {}", theirVersionsUrl: Any, ourToken: Any)
    handshake(ourToken, theirNewToken, theirVersionsUrl, client.updateCredentials, errIfNotRegistered(_, WaitingForRegistrationRequest))
  }

  private def handshake(ourToken: AuthToken[Ours], theirNewToken: AuthToken[Theirs], theirVersionsUrl: Uri,
    credentialExchange: (Url, AuthToken[Ours], Creds[Ours]) => Future[RegistrationError \/ Creds[Theirs]],
    registrationCheck: GlobalPartyId => Future[RegistrationError \/ Unit] )
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]]= {

    def getCredsEndpoint(verDet: VersionDetails) = Future.successful(
      verDet.endpoints.find(_.identifier == Credentials) \/> {
        logger.debug("Credentials endpoint not found in retrieved endpoints for version {}", verDet.version)
        SendingCredentialsFailed: RegistrationError
      }
    )

      (for {
      verDet <- result(getTheirDetails(ourVersion, ourToken, theirVersionsUrl, initiatedByUs = true))
      credEp <- result(getCredsEndpoint(verDet))
      theirCreds <- result {
        logger.debug(s"issuing new token for party with initial authorization token: '$theirNewToken'")
        credentialExchange(credEp.url, ourToken, generateCreds(theirNewToken))
      }
      _ <- result(registrationCheck(theirCreds.globalPartyId))
      _ <- result(
        repo.persistInfoAfterConnectToThem(ourVersion, theirNewToken,
          theirCreds, verDet.endpoints).map(_.right)
      )
    } yield theirCreds).run
  }

  private def getTheirDetails(version: VersionNumber, token: AuthToken[Ours], theirVersionsUrl: Uri, initiatedByUs: Boolean)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ VersionDetails] = {

    def findCommonVersion(versionResp: List[Version]): Future[RegistrationError \/ Version] = {
      logger.debug(s"looking for $version, versionResp: $versionResp")
      versionResp.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None =>
          if (initiatedByUs) Future.successful(-\/(CouldNotFindMutualVersion))
          else if (ourVersion != version) Future.successful(-\/(SelectedVersionNotHostedByUs(version)))
          else Future.successful(-\/(SelectedVersionNotHostedByThem(version)))
      }
    }

    (for {
      theirVers <- resultWithRecover(client.getTheirVersions(theirVersionsUrl, token), VersionsRetrievalFailed)
      commonVer <- result(findCommonVersion(theirVers))
      theirVerDetails <- resultWithRecover(client.getTheirVersionDetails(commonVer.url, token), VersionDetailsRetrievalFailed)
    } yield theirVerDetails).run
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
  private val logger = Logger(getClass)

  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def resultWithRecover[L, T](future: Future[L \/ T], error: L)(implicit ec: ExecutionContext): Result[L, T] =
    EitherT { future.recover {
      case exception => logger.error(s"recovering future with $error from $exception", exception.getCause)
        -\/(error)
    } }

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}
