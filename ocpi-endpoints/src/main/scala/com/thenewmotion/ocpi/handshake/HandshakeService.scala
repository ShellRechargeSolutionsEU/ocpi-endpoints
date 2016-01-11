package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi
import com.thenewmotion.ocpi._
import Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.{EndpointIdentifier, VersionDetailsResp}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{CredsResp, Creds}
import spray.http.Uri
import scala.concurrent.{Future, ExecutionContext}
import scalaz.Scalaz._
import scalaz._

abstract class HandshakeService(implicit system: ActorRefFactory) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  def client: HandshakeClient = new HandshakeClient

  def reactToHandshakeRequest(version: String, existingTokenToConnectToUs: String, credsToConnectToThem: Creds)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"Handshake initiated: token for party to connect to us is $existingTokenToConnectToUs, " +
      s"chosen version: $version.\nCredentials for us: $credsToConnectToThem")
    val result = for {
      res <- getTheirDetails(version, credsToConnectToThem.token, Uri(credsToConnectToThem.url))
    } yield res
    result.map {
      case -\/(error) =>
        logger.error(s"error getting versions information: $error")
        -\/(error)
      case \/-(verDetails) =>
        verDetails.data.endpoints.map(ep =>
          persistTheirEndpoint(version, existingTokenToConnectToUs, credsToConnectToThem.token, ep.identifier.name, ep.url))
        val newTokenToConnectToUs = ApiTokenGenerator.generateToken
        logger.debug(s"issuing new token for party '${credsToConnectToThem.business_details.name}'")
        persistTheirPrefs(version, existingTokenToConnectToUs, credsToConnectToThem)
        persistNewTokenToConnectToUs(existingTokenToConnectToUs, newTokenToConnectToUs)
        \/-(generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl))
    }
  }

  /** Returns the new credentials to connect to them */
  def initiateHandshakeProcess(tokenToConnectToThem: String, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    logger.info(s"initiate handshake process with: $theirVersionsUrl, $tokenToConnectToThem")
    val newTokenToConnectToUs = ApiTokenGenerator.generateToken
    logger.debug(s"issuing new token for party with initial authorization token: '$tokenToConnectToThem'")

    //TODO: It should all be abstract method of the library so applications have more freedom - TNM-1986
    def persist(newCredToConnectToThem: Creds, theirVerDet: VersionDetailsResp): HandshakeError \/ Creds = {
      // register their new token and the party
      val pToken = persistTokenForNewParty(newCredToConnectToThem.business_details.name, newTokenToConnectToUs,
        ourVersion, newCredToConnectToThem.party_id, newCredToConnectToThem.country_code)
      // persist their credentials
      lazy val pPrefs = persistTheirPrefs (ocpi.ourVersion, newTokenToConnectToUs, newCredToConnectToThem)
      // persist their endpoints (theirVerDet)
      lazy val pEndpError = theirVerDet.data.endpoints
        .map(ep => persistTheirEndpoint(
          ocpi.ourVersion, newTokenToConnectToUs, newCredToConnectToThem.token, ep.identifier.name, ep.url))

      // returns the 1st error found
      pToken match {
        case -\/(tokenError) => -\/(tokenError)
        case _ => pPrefs match {
          case -\/(prefsError) => -\/(prefsError)
          case _ => pEndpError.filter(_.isLeft) match {
            case -\/(firstEndpointError)::tail => -\/(firstEndpointError)
            case _ => \/-(newCredToConnectToThem)
          }}}
    }

    (for {
      theirVerDet <- result(getTheirDetails(ocpi.ourVersion, tokenToConnectToThem, theirVersionsUrl))
      theirCredEndpoint = theirVerDet.data.endpoints.filter(_.identifier == EndpointIdentifier.Credentials).head
        newCredToConnectToThem <- result(client.sendCredentials(theirCredEndpoint.url, tokenToConnectToThem,
        generateCredsToConnectToUs(newTokenToConnectToUs, ourVersionsUrl)))
      p <- result(Future.successful(persist(newCredToConnectToThem.data, theirVerDet))) //TODO: TNM-1986
    } yield newCredToConnectToThem.data).run


  }

  /** Get versions, choose the one that match with the 'version' parameter, request the details of this version,
  * and return them if no error happened, otherwise return the error. It doesn't store them cause could be the party
  * is not still registered
  */
  private[ocpi] def getTheirDetails(version: String, tokenToConnectToThem: String, theirVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {

    def findCommonVersion(versionResp: Versions.VersionsResp): Future[HandshakeError \/ Versions.Version] = {
      versionResp.data.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None => Future.successful(-\/(SelectedVersionNotHosted))
      }
    }

    (for {
      theirVers <- result(client.getTheirVersions(theirVersionsUrl, tokenToConnectToThem))
      ver <- result(findCommonVersion(theirVers))
      theirVerDetails <- result(client.getTheirVersionDetails(ver.url, tokenToConnectToThem))
    } yield theirVerDetails).run
  }


  private[ocpi] def generateCredsToConnectToUs(tokenToConnectToUs: String, ourVersionsUrl: Uri): Creds = {
    import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails

    Creds(tokenToConnectToUs, ourVersionsUrl.toString(), BusinessDetails(ourPartyName, ourLogo, ourWebsite), ourPartyId, ourCountryCode)
  }

  def persistTheirPrefs(version: String, tokenToConnectToUs: String, credsToConnectToThem: Creds): HandshakeError \/ Unit

  def persistNewTokenToConnectToUs(oldToken: String, newToken: String): HandshakeError \/ Unit

  def persistTokenForNewParty(newPartyName: String, newToken: String,
    selectedVersion: String, partyId: String, countryCode: String): HandshakeError \/ Unit

  def persistTheirEndpoint(version: String, existingTokenToConnectToUs: String,
    tokenToConnectToThem: String, endpName: String, url: Url): HandshakeError \/ Unit

  def ourPartyName: String

  def ourLogo: Option[Url]

  def ourWebsite: Option[Url]

  def ourVersionsUrl: Uri

  def ourPartyId: String

  def ourCountryCode: String
}

object ApiTokenGenerator {

  import java.security.SecureRandom

  val TOKEN_LENGTH = 32
  val TOKEN_CHARS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"
  val secureRandom = new SecureRandom()

  def generateToken: String =
    generateToken(TOKEN_LENGTH)

  def generateToken(tokenLength: Int): String =
    if (tokenLength == 0) ""
    else TOKEN_CHARS(secureRandom.nextInt(TOKEN_CHARS.length())) +
      generateToken(tokenLength - 1)

}

trait FutureEitherUtils {
  type Result[E, T] = EitherT[Future, E, T]

  def result[L, T](future: Future[L \/ T]): Result[L, T] = EitherT(future)

  def futureLeft[L, T](left: L): Future[L \/ T] =
    Future.successful(-\/(left))

  def futureRight[L, T](right: T): Future[L \/ T] =
    Future.successful(\/-(right))
}