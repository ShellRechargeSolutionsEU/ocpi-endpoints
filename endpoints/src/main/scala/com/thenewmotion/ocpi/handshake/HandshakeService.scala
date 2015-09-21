package com.thenewmotion.ocpi.handshake


import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.handshake.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions
import com.thenewmotion.ocpi.msgs.v2_0.Versions.VersionDetailsResp
import spray.http.Uri

import scala.concurrent.{Await, Future}
import scalaz.Scalaz._
import scalaz._


class HandshakeService(client: HandshakeClient, cdh: HandshakeDataHandler)
  extends FutureEitherUtils {

  import scala.concurrent.duration._
  private val logger = Logger(getClass)

  def registerVersionsEndpoint(version: String, auth: String, creds: Credentials): HandshakeError \/ Creds = {
    logger.info(s"register endpoint: $version, $auth, $creds")
    val result = for {
      commPrefs <- cdh.persistClientPrefs(version, auth, creds)
      res <- completeRegistration(version, auth, Uri(creds.versions_url))
    } yield res
    result match {
      case -\/(_) => -\/(CouldNotRegisterParty)
      case _ =>
        val newToken = ApiTokenGenerator.generateToken
        cdh.persistNewToken(auth, newToken)
        \/-(newCredentials(newToken))
    }
  }


  private[ocpi] def completeRegistration(version: String, auth: String, uri: Uri): HandshakeError \/ VersionDetailsResp = {
    import client.system.dispatcher

    def findVersion(versionResp: Versions.VersionsResp): Future[HandshakeError \/ Versions.Version] = {
      versionResp.data.find(_.version == version) match {
        case Some(ver) => Future.successful(\/-(ver))
        case None => Future.successful(-\/(SelectedVersionNotHosted))
      }
    }
    val res = (for {
       vers <- result(client.getVersions(uri, auth))
      ver <- result(findVersion(vers))
      verDetails <- result(client.getVersionDetails(ver.url, auth))
      unit = verDetails.data.endpoints.map(ep => cdh.persistEndpoint(version, auth, ep.identifier.name, ep.url))
    } yield verDetails).run
    Await.result(res, 5.seconds)
  }


  private[ocpi] def newCredentials(token: String): Creds = {
    val versionsUrl = s"http://${cdh.config.host}:${cdh.config.port}/${cdh.config.namespace}"
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
