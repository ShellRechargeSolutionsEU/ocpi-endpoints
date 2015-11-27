package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import com.thenewmotion.ocpi._
import Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes._
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.VersionDetailsResp
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import spray.http.Uri
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._

abstract class HandshakeService(implicit system: ActorRefFactory) extends FutureEitherUtils {

  private val logger = Logger(getClass)

  def client: HandshakeClient = new HandshakeClient

  def startHandshake(version: String, auth: String, creds: Creds, versionsUrl: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {

    logger.info(s"register endpoint: $version, $auth, $creds")
    val result = for {
      commPrefs <- Future.successful(persistClientPrefs(version, auth, creds))
      res <- completeRegistration(version, creds.token, Uri(creds.url))
    } yield res
    result.map {
      case -\/(_) => -\/(CouldNotRegisterParty)
      case _ =>
        val newToken = ApiTokenGenerator.generateToken
        logger.debug(s"issuing new token for party '${creds.business_details.name}'")
        persistNewToken(auth, newToken)
        \/-(newCredentials(newToken, versionsUrl))
    }
  }


  private[ocpi] def completeRegistration(version: String, auth: String, uri: Uri)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {

    def findVersion(versionResp: Versions.VersionsResp): Future[HandshakeError \/ Versions.Version] = {
      versionResp.data.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None => Future.successful(-\/(SelectedVersionNotHosted))
      }
    }

    (for {
      vers <- result(client.getVersions(uri, auth))
      ver <- result(findVersion(vers))
      verDetails <- result(client.getVersionDetails(ver.url, auth))
      unit = verDetails.data.endpoints.map(ep => persistEndpoint(version, auth, ep.identifier.name, ep.url))
    } yield verDetails).run
  }


  private[ocpi] def newCredentials(token: String, uri: Uri): Creds = {
    import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails

    val versionsUri = uri.withPath(uri.path)

    Creds(token, versionsUri.toString(), BusinessDetails(partyname, None, None))
  }

  def persistClientPrefs(version: String, auth: String, creds: Creds): PersistenceError \/ Unit

  def persistNewToken(auth: String, newToken: String): PersistenceError \/ Unit

  def persistEndpoint(version: String, auth: String, name: String, url: Url): PersistenceError \/ Unit

  def partyname: String
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