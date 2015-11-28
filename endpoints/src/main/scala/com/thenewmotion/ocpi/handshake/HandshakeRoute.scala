package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, versionsUrl: String, currentTime: => DateTime = DateTime.now) extends JsonApi {

  def route(version: Version, auth: AuthToken)(implicit ec: ExecutionContext) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

    (post & extract(_.request.uri)) { credentialsUrl =>
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
