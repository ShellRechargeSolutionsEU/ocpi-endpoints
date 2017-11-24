package com.thenewmotion.ocpi
package registration

import msgs.OcpiStatusCode.GenericSuccess

import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import _root_.akka.stream.Materializer
import com.thenewmotion.ocpi.common.{ErrRespMar, OcpiDirectives, SuccessRespMar}
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import msgs.Ownership.{Ours, Theirs}
import msgs.{GlobalPartyId, SuccessResp}

object RegistrationRoute {
  def apply(
    service: RegistrationService
  )(
    implicit mat: Materializer,
    errorM: ErrRespMar,
    succOurCredsM: SuccessRespMar[Creds[Ours]],
    succUnitM: SuccessRespMar[Unit],
    theirCredsU: FromEntityUnmarshaller[Creds[Theirs]]
  ): RegistrationRoute = new RegistrationRoute(service)
}

class RegistrationRoute private[ocpi](
  service: RegistrationService
)(
  implicit mat: Materializer,
  errorM: ErrRespMar,
  succOurCredsM: SuccessRespMar[Creds[Ours]],
  succUnitM: SuccessRespMar[Unit],
  theirCredsU: FromEntityUnmarshaller[Creds[Theirs]]
) extends OcpiDirectives {

  def apply(
    accessedVersion: VersionNumber,
    user: GlobalPartyId
  )(
    implicit ec: ExecutionContext
  ): Route = {
    post {
      entity(as[Creds[Theirs]]) { credsToConnectToThem =>
        complete {
          service
            .reactToNewCredsRequest(user, accessedVersion, credsToConnectToThem)
            .mapRight(x => SuccessResp(GenericSuccess, data = x))
        }
      }
    } ~
    get {
      complete {
        service
          .credsToConnectToUs(user)
          .mapRight(x => SuccessResp(GenericSuccess, data = x))
      }
    } ~
    put {
      entity(as[Creds[Theirs]]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(user, accessedVersion, credsToConnectToThem)
            .mapRight(x => SuccessResp(GenericSuccess, data = x))
        }
      }
    } ~
    delete {
      complete {
        service
          .reactToDeleteCredsRequest(user)
          .mapRight(x => SuccessResp(GenericSuccess, data = x))
      }
    }
  }
}
