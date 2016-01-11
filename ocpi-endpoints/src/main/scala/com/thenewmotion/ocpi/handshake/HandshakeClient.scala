package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi._
import com.thenewmotion.ocpi.handshake.Errors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{Url, SuccessResp}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{CredsResp, Creds}
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import spray.client.pipelining._
import spray.http._
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Try
import scalaz.Scalaz._
import scalaz._

class HandshakeClient(implicit refFactory: ActorRefFactory) {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  private val logger = Logger(getClass)

  // setup request/response logging
  val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  implicit val timeout = Timeout(10.seconds)

  def request(tokenToConnectToThem: String)(implicit ec: ExecutionContext) = (
    addCredentials(GenericHttpCredentials("Token", tokenToConnectToThem, Map()))
      ~> logRequest
      ~> sendReceive
      ~> logResponse
    )

  private def bimap[T, M](f: Future[T])(pf: PartialFunction[Try[T], M])(implicit ec: ExecutionContext): Future[M] = {
    val p = Promise[M]()
    f.onComplete(r => p.complete(Try(pf(r))))
    p.future
  }

  def getTheirVersions(uri: Uri, token: String)(implicit ec: ExecutionContext): Future[HandshakeError \/ VersionsResp] = {
    val pipeline = request(token) ~> unmarshal[VersionsResp]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case scala.util.Success(versions) => \/-(versions)
      case scala.util.Failure(_) =>
        logger.error(s"Could not retrieve the versions information from $uri with token $token")
        -\/(VersionsRetrievalFailed)
    }
  }

  def getTheirVersionDetails(uri: Uri, token: String)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {
    val pipeline = request(token) ~> unmarshal[VersionDetailsResp]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case scala.util.Success(versionDet) => \/-(versionDet)
      case scala.util.Failure(_) =>
        logger.error(s"Could not retrieve the version details from $uri with token $token")
        -\/(VersionDetailsRetrievalFailed)
    }
  }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: String, credToConnectToUs: Creds)
    (implicit ec: ExecutionContext): Future[HandshakeError \/ CredsResp] = {
    val pipeline = request(tokenToConnectToThem) ~> unmarshal[CredsResp]
    val resp = pipeline(Post(theirCredUrl, credToConnectToUs))
    bimap(resp) {
      case scala.util.Success(theirCreds) => \/-(theirCreds)
      case scala.util.Failure(_) =>
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
             s"$tokenToConnectToThem when sending our credentials $credToConnectToUs")
        -\/(SendingCredentialsFailed)
    }
  }
}

