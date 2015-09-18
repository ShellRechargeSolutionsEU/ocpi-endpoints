package com.thenewmotion.ocpi

import akka.actor.ActorSystem
import akka.util.Timeout
import com.thenewmotion.ocpi.credentials.CredentialsErrors._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.SuccessResp
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import com.typesafe.scalalogging.LazyLogging
import spray.http._
import spray.httpx.unmarshalling._
import spray.client.pipelining._
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

class OcpiClient( val system: ActorSystem) extends LazyLogging {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  // setup request/response logging
  val logRequest: HttpRequest => HttpRequest = { r => logger.debug(r.toString); r }
  val logResponse: HttpResponse => HttpResponse = { r => logger.debug(r.toString); r }

  implicit val sys = system
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  def request(auth: String) = (
    addCredentials(GenericHttpCredentials("Token", auth, Map()))
      ~> logRequest
      ~> sendReceive
      ~> logResponse
    )

  def unmarshalToOption[T](implicit unmarshaller: FromResponseUnmarshaller[T]):
  Future[HttpResponse] => Future[Option[T]] = {

    _.map { res =>
      if (res.status.isFailure) None
      else Some(unmarshal[T](unmarshaller)(res))
    }
  }

  def getVersions(uri: Uri, auth: String): Future[RegistrationError \/ VersionsResp] = {
    val pipeline = request(auth) ~> unmarshalToOption[VersionsResp]
    pipeline(Get(uri)) map { toRight(_)(VersionsRetrievalFailed) }
  }

  def getVersionDetails(uri: Uri, auth: String): Future[RegistrationError \/ VersionDetailsResp] = {
    val pipeline = request(auth) ~> unmarshalToOption[VersionDetailsResp]
    pipeline(Get(uri)) map { toRight(_)(VersionDetailsRetrievalFailed) }
  }

  def sendCredentials(uri: Uri, auth: String, creds: Creds): Future[RegistrationError \/ SuccessResp] = {
    val pipeline = request(auth) ~> unmarshalToOption[SuccessResp]
    pipeline(Post(uri, creds)) map { toRight(_)(SendingCredentialsFailed) }
  }

}

//object OcpiClient {
//
//  def apply(implicit system: ActorSystem) = {
//    new OcpiClient()
//  }
//
//}
