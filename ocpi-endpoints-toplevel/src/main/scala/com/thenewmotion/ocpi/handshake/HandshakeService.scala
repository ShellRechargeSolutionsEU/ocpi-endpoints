package com.thenewmotion.ocpi.handshake

import java.security.SecureRandom
import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions
import com.thenewmotion.ocpi.msgs.v2_1.Versions.{Endpoint, EndpointIdentifier, VersionDetails, VersionNumber}
import spray.http.Uri
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

abstract class HandshakeService(
  ourNamespace: String,
  ourPartyName: String,
  ourLogo: Option[Image],
  ourWebsite: Option[Url],
  ourBaseUrl: Uri,
  ourPartyId: String,
  ourCountryCode: String
)(implicit system: ActorRefFactory) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  private[handshake] val client: HandshakeClient = new HandshakeClient

  private val ourVersionsUrl = ourBaseUrl + "/" + ourNamespace + "/" + EndpointIdentifier.Versions.name

  /**
    * React to a handshake request.
    *
    * @return new credentials to connect to us
    */
  def reactToHandshakeRequest(
    version: VersionNumber,
    existingTokenToConnectToUs: String,
    credsToConnectToThem: Creds
  )(implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"Handshake initiated by party: ${credsToConnectToThem.partyId}, " +
      s"using token: $existingTokenToConnectToUs, " +
      s"chosen version: ${version.name}. " +
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
          version, existingTokenToConnectToUs, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints)

        persistResult.bimap(
          e => { logger.error(s"error persisting handshake data: $e"); e },
          _ => generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl)
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
    existingTokenToConnectToUs: String,
    credsToConnectToThem: Creds
  )(implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"Update credentials request sent by ${credsToConnectToThem.partyId} " +
      s"using token: $existingTokenToConnectToUs, for version: ${version.name}. " +
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
          version, existingTokenToConnectToUs, newTokenToConnectToUs,
          credsToConnectToThem, verDetails.endpoints)

        persistResult.bimap(
          e => { logger.error(s"error persisting the update of the credentials: $e"); e },
          _ => generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl)
        )
    }
  }

  /**
    * Initiate handshake process.
    *
    * @return new credentials to connect to them
    */
  def initiateHandshakeProcess(partyName: String, countryCode: String, partyId: String,
    tokenToConnectToThem: String, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    logger.info(s"initiate handshake process with: $theirVersionsUrl, $tokenToConnectToThem")
    val newTokenToConnectToUs = ApiTokenGenerator.generateToken
    logger.debug(s"issuing new token for party with initial authorization token: '$tokenToConnectToThem'")

    def theirDetails =
      getTheirDetails(ocpi.ourVersion, tokenToConnectToThem, theirVersionsUrl, initiatedByUs = true)
    def theirCredEp(versionDetails: VersionDetails) =
      versionDetails.endpoints.filter(_.identifier == EndpointIdentifier.Credentials).head
    def theirNewCred(credEp: Url) =
      client.sendCredentials(credEp, tokenToConnectToThem,
        generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl))
    def withCleanup[A, B](f: => Future[A \/ B]): Future[A \/ B]  = f.map {
      case disj if disj.isLeft => removePartyPendingRegistration(newTokenToConnectToUs); disj
      case disj  => disj
    }
    def persist(creds: Creds, endpoints: Iterable[Endpoint]) =
      persistHandshakeInitResult(ocpi.ourVersion, newTokenToConnectToUs, creds, endpoints)

    (for {
      verDet <- result(theirDetails)
      credEndpoint = theirCredEp(verDet)
      _ <- result(Future.successful(persistPartyPendingRegistration(partyName, countryCode, partyId, newTokenToConnectToUs)))
      newCredToConnectToThem <- result(withCleanup(theirNewCred(credEndpoint.url)))
      _ <- result(Future.successful(persist(newCredToConnectToThem, verDet.endpoints)))
    } yield newCredToConnectToThem).run
  }

  /**
    * Get versions, choose the one that match with the 'version' parameter, request the details of this version,
    * and return them if no error happened, otherwise return the error. It doesn't store them cause could be the party
    * is not still registered
    */
  private def getTheirDetails(version: VersionNumber, tokenToConnectToThem: String, theirVersionsUrl: Uri, initiatedByUs: Boolean)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetails] = {

    def findCommonVersion(versionResp: List[Versions.Version]): Future[HandshakeError \/ Versions.Version] = {
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


  private def generateCredsToConnectToUs(tokenToConnectToUs: String, ourVersionsUrl: Uri): Creds = {
    import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails

    Creds(tokenToConnectToUs, ourVersionsUrl.toString(), BusinessDetails(ourPartyName, ourLogo, ourWebsite), ourPartyId, ourCountryCode)
  }

  protected def persistHandshakeReactResult(
    version: VersionNumber,
    existingTokenToConnectToUs: String,
    newTokenToConnectToUs: String,
    credsToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistUpdateCredsResult(
    version: VersionNumber,
    existingTokenToConnectToUs: String,
    newTokenToConnectToUs: String,
    credsToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistHandshakeInitResult(
    version: VersionNumber,
    newTokenToConnectToUs: String,
    newCredToConnectToThem: Creds,
    endpoints: Iterable[Endpoint]
  ): HandshakeError \/ Unit

  protected def persistPartyPendingRegistration(
    partyName: String,
    countryCode: String,
    partyId: String,
    newTokenToConnectToUs: String
  ): HandshakeError \/ Unit

  protected def removePartyPendingRegistration(
    tokenToConnectToUs: String
  ): HandshakeError \/ Unit

  def credsToConnectToUs(tokenToConnectToUs: String): HandshakeError \/ Creds
}

object ApiTokenGenerator {

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken: String = generateToken(TOKEN_LENGTH)

  def generateToken(length: Int): String =
    (1 to length).map(_ => TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length))).mkString
}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}