package com.thenewmotion.ocpi.handshake


import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.handshake.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.VersionDetailsResp
import spray.http.Uri

import scala.concurrent.{ExecutionContext, Future}
import scalaz.Scalaz._
import scalaz._


class HandshakeService(client: HandshakeClient, cdh: HandshakeDataHandler)
  extends FutureEitherUtils {

  private val logger = Logger(getClass)

  def registerVersionsEndpoint(version: String, auth: String, creds: Credentials)(implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    logger.info(s"register endpoint: $version, $auth, $creds")
    val result = for {
      commPrefs <- Future.successful(cdh.persistClientPrefs(version, auth, creds))
      res <- completeRegistration(version, auth, Uri(creds.versions_url))
    } yield res
    result.map {
      case -\/(_) => -\/(CouldNotRegisterParty)
      case _ =>
        val newToken = ApiTokenGenerator.generateToken
        logger.debug(s"issuing new token for party '${creds.business_details.name}'")
        cdh.persistNewToken(auth, newToken)
        \/-(newCredentials(newToken))
    }
  }


  private[ocpi] def completeRegistration(version: String, auth: String, uri: Uri)(implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {

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
      unit = verDetails.data.endpoints.map(ep => cdh.persistEndpoint(version, auth, ep.identifier.name, ep.url))
    } yield verDetails).run
  }


  private[ocpi] def newCredentials(token: String): Creds = {
    val versionsUrl = s"http://${cdh.config.host}:${cdh.config.port}/${cdh.config.namespace}/${cdh.config.versionsEndpoint}"
    import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails
    Creds(token, versionsUrl, BusinessDetails(cdh.config.partyname, None, None))
  }
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
