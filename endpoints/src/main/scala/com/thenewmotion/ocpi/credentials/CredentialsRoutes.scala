package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.{OcpiClient, CurrentTimeComponent}
import com.thenewmotion.ocpi.credentials.CredentialsErrors.CouldNotRegisterParty
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.SuccessResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import spray.routing.HttpService
import scalaz._

trait CredentialsRoutes extends HttpService with CurrentTimeComponent {

  def cdh: CredentialsDataHandler
  def client: OcpiClient


  def credentialsRoute(version: String, auth: String) = {
    import spray.httpx.SprayJsonSupport._
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

    path(cdh.config.endpoint) {
        post {
          entity(as[Creds]) { clientCreds =>
            val handshakeService = new HandshakeService(client, cdh)
            handshakeService.registerVersionsEndpoint(version, auth, Credentials.fromOcpiClass(clientCreds)) match {
              case -\/(_) => reject()
              case \/-(newCreds) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
                currentTime.instance, newCreds))
            }
          }
      }
    }
  }
}
