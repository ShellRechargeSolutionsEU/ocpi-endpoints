package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scala.concurrent._
import spray.routing.{Route, Rejection}
import scalaz._
import spray.routing.directives.FutureDirectives

case class HandshakeErrorRejection(error: HandshakeError) extends Rejection

trait HandshakeApi extends JsonApi {
  private val logger = Logger(getClass)

  protected def leftToRejection[T](errOrX: HandshakeError \/ T)(f: T => Route): Route =
    errOrX match {
      case -\/(err) => logger.error(s"HandshakeErrorRejection just happened with reason: ${err.reason}"); reject(HandshakeErrorRejection(err))
      case \/-(res) => f(res)
    }

  protected def futLeftToRejection[T](errOrX: Future[HandshakeError \/ T])(f: T => Route)
    (implicit ec: ExecutionContext): Route = {
    FutureDirectives.onSuccess(errOrX) {
      case -\/(err) => logger.error(s"HandshakeErrorRejection just happened with reason: ${err.reason}"); reject(HandshakeErrorRejection(err))
      case \/-(res) => f(res)
    }
  }
}

class HandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends HandshakeApi {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) =
    handleRejections(HandshakeRejectionHandler.Default)(routeWithoutRH(accessedVersion, tokenToConnectToUs))

  private[handshake] def routeWithoutRH(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds]) { credsToConnectToThem =>
        futLeftToRejection(service.reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)) {
          newCredsToConnectToUs =>
            complete(CredsResp(GenericSuccess.code, Some(GenericSuccess.default_message),
              currentTime, newCredsToConnectToUs))
        }
      }
    } ~
      get {
        leftToRejection(service.findRegisteredCredsToConnectToUs(tokenToConnectToUs)) { credsToConnectToUs =>
            complete(CredsResp(GenericSuccess.code, None, currentTime, credsToConnectToUs))
        }
      } ~
      put {
        entity(as[Creds]) { credsToConnectToThem =>
          onSuccess(service.reactToUpdateCredsRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)) {
            case -\/(_) => reject()
            case \/-(newCredsToConnectToUs) => complete(CredsResp(GenericSuccess.code, Some(GenericSuccess.default_message),
              currentTime, newCredsToConnectToUs))
          }
        }
      }
  }
}

class InitiateHandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends HandshakeApi {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
  import com.thenewmotion.ocpi.msgs.v2_0.Versions._

  def route(implicit ec: ExecutionContext) = handleRejections(HandshakeRejectionHandler.Default)(routeWithoutRH)

  private[handshake] def routeWithoutRH(implicit ec: ExecutionContext) = {
    post {
      entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
        futLeftToRejection(service.initiateHandshakeProcess(theirVersionsUrlInfo.token, theirVersionsUrlInfo.url)) {
          newCredToConnectToThem =>
            complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
              currentTime, newCredToConnectToThem))
        }
      }
    }
  }
}