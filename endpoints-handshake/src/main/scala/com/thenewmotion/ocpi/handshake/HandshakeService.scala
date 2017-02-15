package com.thenewmotion.ocpi
package handshake

import java.security.SecureRandom
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
import HandshakeError._
import msgs._

abstract class HandshakeService(
  ourNamespace: String,
  ourPartyName: String,
  ourLogo: Option[Image],
  ourWebsite: Option[Url],
  ourBaseUrl: Uri,
  ourPartyId: PartyId,
  ourCountryCode: CountryCode
)(implicit http: HttpExt) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  private[handshake] val client: HandshakeClient = new HandshakeClient

  def ourVersionsUrl: Url

  /**
    * React to a handshake request.
    *
    * @return new credentials to connect to us
    */
  def reactToHandshakeRequest(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    credsToConnectToThem: Creds[OurAuthToken]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ Creds[TheirAuthToken]] = {

    logger.info(s"Handshake initiated by party: $globalPartyId, " +
      s"chosen version: $version. " +
      s"Credentials for us: $credsToConnectToThem")

    val details = getTheirDetails(
      version, credsToConnectToThem.token, Uri(credsToConnectToThem.url), initiatedByUs = false)

    details map {
      case e @ -\/(error) =>
        logger.error(s"error getting versions information: $error"); e
      case \/-(verDetails) =>
        logger.debug(s"issuing new token for party id '${credsToConnectToThem.partyId}'")
        val newTokenToConnectToUs = ApiTokenGenerator.generateToken

        val persistResult = persistHandshakeReactResult(
          version, globalPartyId, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints)

        persistResult.bimap(
          e => { logger.error(s"error persisting handshake data: $e"); e },
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
    credsToConnectToThem: Creds[OurAuthToken]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ Creds[TheirAuthToken]] = {

    logger.info(s"Update credentials request sent by ${credsToConnectToThem.partyId} " +
      s"for version: $version. " +
      s"New credentials for us: $credsToConnectToThem")

    val details = getTheirDetails(
      version, credsToConnectToThem.token, Uri(credsToConnectToThem.url), initiatedByUs = false)

    details map {
      case e @ -\/(error) =>
        logger.error(s"error getting versions information: $error"); e
      case \/-(verDetails) =>
        logger.debug(s"issuing new token for party id '${credsToConnectToThem.partyId}'")
        val newTokenToConnectToUs = ApiTokenGenerator.generateToken

        val persistResult = persistUpdateCredsResult(
          version, globalPartyId, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints)

        persistResult.bimap(
          e => { logger.error(s"error persisting the update of the credentials: $e"); e },
          _ => generateCredsToConnectToUs(newTokenToConnectToUs)
        )
    }
  }

  /**
    * Initiate handshake process.
    *
    * @return new credentials to connect to them
    */
  def initiateHandshakeProcess(partyName: String, globalPartyId: GlobalPartyId,
                               tokenToConnectToThem: OurAuthToken, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ Creds[OurAuthToken]] = {
    logger.info(s"initiate handshake process with: $theirVersionsUrl, $tokenToConnectToThem")
    val newTokenToConnectToUs = ApiTokenGenerator.generateToken
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
    def persist(creds: Creds[OurAuthToken], endpoints: Iterable[Endpoint]) =
      persistHandshakeInitResult(ourVersion, globalPartyId, newTokenToConnectToUs, creds, endpoints)

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
  private def getTheirDetails(version: VersionNumber, tokenToConnectToThem: OurAuthToken, theirVersionsUrl: Uri, initiatedByUs: Boolean)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ VersionDetails] = {

    def findCommonVersion(versionResp: List[Version]): Future[HandshakeError \/ Version] = {
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

  protected def getTheirAuthToken(globalPartyId: GlobalPartyId): HandshakeError \/ TheirAuthToken

  def credsToConnectToUs(globalPartyId: GlobalPartyId): HandshakeError \/ Creds[TheirAuthToken] = {
    for {
      authToken <- getTheirAuthToken(globalPartyId)
    } yield generateCredsToConnectToUs(authToken)
  }

  private def generateCredsToConnectToUs(tokenToConnectToUs: TheirAuthToken): Creds[TheirAuthToken] = {
    import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
    Creds(tokenToConnectToUs, ourVersionsUrl.toString(),
      BusinessDetails(ourPartyName, ourLogo, ourWebsite), ourPartyId, ourCountryCode)
  }

  protected def persistHandshakeReactResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken,
    credsToConnectToThem: Creds[OurAuthToken],
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistUpdateCredsResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken,
    credsToConnectToThem: Creds[OurAuthToken],
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistHandshakeInitResult(
    version: VersionNumber,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken,
    newCredToConnectToThem: Creds[OurAuthToken],
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistPartyPendingRegistration(
    partyName: String,
    globalPartyId: GlobalPartyId,
    newTokenToConnectToUs: TheirAuthToken
  ): HandshakeError \/ Unit

  protected def removePartyPendingRegistration(
    globalPartyId: GlobalPartyId
  ): HandshakeError \/ Unit
}

object ApiTokenGenerator {

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken: TheirAuthToken = generateToken(TOKEN_LENGTH)

  def generateToken(length: Int): TheirAuthToken =
    TheirAuthToken((1 to length).map(_ => TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length))).mkString)
}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}
