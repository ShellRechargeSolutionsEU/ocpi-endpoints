package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi
import com.thenewmotion.ocpi._
import Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.{EndpointIdentifier, VersionDetailsResp}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import spray.http.Uri
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

abstract class HandshakeService(implicit system: ActorRefFactory) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  def client: HandshakeClient = new HandshakeClient

  def reactToHandshakeRequest(version: String, auth: String, creds: Creds, versionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"Handshake initiated: client's auth is $auth, chosen version: $version.\nCredentials for us: $creds")
    val result = for {
      res <- completeRegistration(version, auth, creds.token, Uri(creds.url))
    } yield res
    result.map {
      case -\/(_) => -\/(CouldNotRegisterParty)  //why do you send back a different error to the registered one?
      case _ =>
        val newToken = ApiTokenGenerator.generateToken
        logger.debug(s"issuing new token for party '${creds.business_details.name}'")
        persistClientPrefs(version, auth, creds)
        persistNewToken(auth, newToken)
        \/-(newCredentials(newToken, versionsUrl))
    }
  }

  def initiateHandshakeProcess(auth: String, clientVersionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    logger.info(s"initiate handshake process with: $clientVersionsUrl, $auth")
    val tokenForClient = ApiTokenGenerator.generateToken
    logger.debug(s"issuing new token for party with initial authorization token: '$auth'")

    getHostedVersionsUrl match {
      case -\/(error) => Future.successful(-\/(error))
      case \/-(hostedVersionsUri) =>

        (for {
          persistence <- result(Future.successful(persistNewToken(auth, tokenForClient)))
          versionDet <- result(completeRegistration(ocpi.version ,auth, tokenForClient, clientVersionsUrl))
          credEndpoint = versionDet.data.endpoints.filter(_.identifier == EndpointIdentifier.Credentials).head
          credentials <- result(client.sendCredentials(credEndpoint.url, auth, newCredentials(tokenForClient, hostedVersionsUri)))
        } yield credentials).run
    }
  }

  /** Get versions, choose the one that match with the 'version' parameter, request the details of this version,
  * persist them (the how is defined in the application is making use of the library)
  * and return them if no error happened, otherwise return the error
  */
  private[ocpi] def completeRegistration(version: String, auth_for_server_api: String, auth_for_client_api: String, versionsUri: Uri)(implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {

    def findVersion(versionResp: Versions.VersionsResp): Future[HandshakeError \/ Versions.Version] = {
      versionResp.data.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None => Future.successful(-\/(SelectedVersionNotHosted))
      }
    }

    (for {
      vers <- result(client.getVersions(versionsUri, auth_for_client_api))
      ver <- result(findVersion(vers))
      verDetails <- result(client.getVersionDetails(ver.url, auth_for_client_api))
      unit = verDetails.data.endpoints.map(ep => persistEndpoint(version, auth_for_server_api, auth_for_client_api, ep.identifier.name, ep.url))
    } yield verDetails).run
  }


  private[ocpi] def newCredentials(token: String, versionsUri: Uri): Creds = {
    import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails

    Creds(token, versionsUri.toString(), BusinessDetails(partyname, logo, website))
  }

  def persistClientPrefs(version: String, auth: String, creds: Creds): PersistenceError \/ Unit

  def persistNewToken(auth: String, newToken: String): PersistenceError \/ Unit

  def persistEndpoint(version: String, auth_for_server_api: String, auth_for_client_api: String, name: String, url: Url): PersistenceError \/ Unit

  def partyname: String

  def logo: Option[Url]

  def website: Option[Url]

  def getHostedVersionsUrl: PersistenceError \/ Uri
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