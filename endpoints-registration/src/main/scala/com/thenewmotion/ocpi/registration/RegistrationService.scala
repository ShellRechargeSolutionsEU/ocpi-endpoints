package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import msgs.Versions.EndpointIdentifier.Credentials
import msgs.Versions.{Endpoint, Version, VersionDetails, VersionNumber}
import msgs.v2_1.CommonTypes._
import msgs.v2_1.Credentials.Creds
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._
import RegistrationError._
import msgs.Ownership.{Ours, Theirs}
import msgs._

abstract class RegistrationService(
  ourPartyName: String,
  ourLogo: Option[Image],
  ourWebsite: Option[Url],
  ourBaseUrl: Uri,
  ourGlobalPartyId: GlobalPartyId
)(implicit http: HttpExt) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  private[registration] val client: RegistrationClient = new RegistrationClient

  def ourVersionsUrl: Url

  /**
    * React to a post credentials request.
    *
    * @return new credentials to connect to us
    */
  def reactToPostCredsRequest(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    credsToConnectToThem: Creds[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {

    logger.info(s"Registration initiated by party: $globalPartyId, " +
      s"chosen version: $version. " +
      s"Credentials for us: $credsToConnectToThem")

    val details = getTheirDetails(
      version, credsToConnectToThem.token, Uri(credsToConnectToThem.url), initiatedByUs = false)

    details map {
      case e @ -\/(error) =>
        logger.error(s"error getting versions information: $error"); e
      case \/-(verDetails) =>
        logger.debug(s"issuing new token for party id '${credsToConnectToThem.globalPartyId}'")
        val newTokenToConnectToUs = AuthToken.generateTheirs

        persistPostCredsResult(
          version, globalPartyId, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints
        ).bimap(
          e => { logger.error(s"error persisting registration data: $e"); e },
          _ => generateCredsToConnectToUs(newTokenToConnectToUs)
        )
    }
  }

  /**
    * React to a update credentials request.
    *
    * @return new credentials to connect to us
    */
  def reactToUpdateCredsRequest(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    credsToConnectToThem: Creds[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {

    logger.info(s"Update credentials request sent by ${credsToConnectToThem.globalPartyId} " +
      s"for version: $version. " +
      s"New credentials for us: $credsToConnectToThem")

    val details = getTheirDetails(
      version, credsToConnectToThem.token, Uri(credsToConnectToThem.url), initiatedByUs = false)

    details map {
      case e @ -\/(error) =>
        logger.error(s"error getting versions information: $error"); e
      case \/-(verDetails) =>
        logger.debug(s"issuing new token for party id '${credsToConnectToThem.globalPartyId}'")
        val newTokenToConnectToUs = AuthToken.generateTheirs

        val persistResult = persistUpdateCredsResult(
          version, globalPartyId, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints)

        persistResult.bimap(
          e => { logger.error(s"error persisting the update of the credentials: $e"); e },
          _ => generateCredsToConnectToUs(newTokenToConnectToUs)
        )
    }
  }

  def initiateRegistrationProcess(partyName: String, globalPartyId: GlobalPartyId,
     tokenToConnectToThem: AuthToken[Ours], theirVersionsUrl: Uri)
     (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Ours]] = {
    logger.info(s"initiate registration process with: $theirVersionsUrl, $tokenToConnectToThem")
    val newTokenToConnectToUs = AuthToken.generateTheirs
    logger.debug(s"issuing new token for party with initial authorization token: '$tokenToConnectToThem'")

    def theirDetails =
      getTheirDetails(ourVersion, tokenToConnectToThem, theirVersionsUrl, initiatedByUs = true)
    def theirCredEp(versionDetails: VersionDetails) =
      versionDetails.endpoints.filter(_.identifier == Credentials).head
    def theirNewCred(credEp: Url) =
      client.sendCredentials(credEp, tokenToConnectToThem,
        generateCredsToConnectToUs(newTokenToConnectToUs))
    def withCleanup[A, B](f: => Future[A \/ B]): Future[A \/ B]  = f.map {
      case disj if disj.isLeft => removePartyPendingRegistration(globalPartyId); disj
      case disj  => disj
    }
    def persist(creds: Creds[Ours], endpoints: Iterable[Endpoint]) =
      persistRegistrationInitResult(ourVersion, globalPartyId, newTokenToConnectToUs, creds, endpoints)

    (for {
      verDet <- result(theirDetails)
      credEndpoint = theirCredEp(verDet)
      _ <- result(Future.successful(persistPartyPendingRegistration(partyName, globalPartyId, newTokenToConnectToUs)))
      newCredToConnectToThem <- result(withCleanup(theirNewCred(credEndpoint.url)))
      _ <- result(Future.successful(persist(newCredToConnectToThem, verDet.endpoints)))
    } yield newCredToConnectToThem).run
  }

  /**
    * Get versions, choose the one that match with the 'version' parameter, request the details of this version,
    * and return them if no error happened, otherwise return the error. It doesn't store them cause could be the party
    * is not still registered
    */
  private def getTheirDetails(version: VersionNumber, tokenToConnectToThem: AuthToken[Ours], theirVersionsUrl: Uri, initiatedByUs: Boolean)
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
      theirVers <- result(client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem))
      ver <- result(findCommonVersion(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(ver.url, tokenToConnectToThem))
    } yield theirVerDetails).run
  }

  def credsToConnectToUs(globalPartyId: GlobalPartyId): RegistrationError \/ Creds[Theirs] = {
    for {
      authToken <- getTheirAuthToken(globalPartyId)
    } yield generateCredsToConnectToUs(authToken)
  }

  private def generateCredsToConnectToUs(tokenToConnectToUs: AuthToken[Theirs]): Creds[Theirs] = {
    import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
    Creds(tokenToConnectToUs, ourVersionsUrl,
      BusinessDetails(ourPartyName, ourLogo, ourWebsite), ourGlobalPartyId)
  }

  protected def getTheirAuthToken(globalPartyId: GlobalPartyId): RegistrationError \/ AuthToken[Theirs]

  protected def persistPostCredsResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs],
    credsToConnectToThem: Creds[Ours],
    endpoints: Iterable[Endpoint]
  ): RegistrationError \/ Unit

  protected def persistUpdateCredsResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs],
    credsToConnectToThem: Creds[Ours],
    endpoints: Iterable[Endpoint]
  ): RegistrationError \/ Unit

  protected def persistRegistrationInitResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs],
    newCredToConnectToThem: Creds[Ours],
    endpoints: Iterable[Endpoint]
  ): RegistrationError \/ Unit

  protected def persistPartyPendingRegistration(
    partyName: String,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: AuthToken[Theirs]
  ): RegistrationError \/ Unit

  protected def removePartyPendingRegistration(
    globalPartyId: GlobalPartyId
  ): RegistrationError \/ Unit
}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}
