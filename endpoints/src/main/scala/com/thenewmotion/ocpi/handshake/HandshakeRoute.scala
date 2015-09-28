package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, namespace: String, versionsEndpoint: String, currentTime: => DateTime = DateTime.now) extends JsonApi {

  def route(version: Version, auth: AuthToken)(implicit ec: ExecutionContext) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

    (post & extract(_.request.uri)) { credentialsUrl =>
      //FIXME:
      val versionsUrl = spray.http.Uri(credentialsUrl.toString().replaceAll("2.0/credentials", "versions"))
      entity(as[Creds]) { clientCreds =>
        onSuccess(service.startHandshake(version, auth, clientCreds, versionsUrl)) {
          case -\/(_) => reject()
          case \/-(newCreds) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
            currentTime, newCreds))
        }
      }
    }
  }
}
