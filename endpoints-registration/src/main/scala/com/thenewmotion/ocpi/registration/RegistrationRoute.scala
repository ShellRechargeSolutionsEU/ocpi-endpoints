package com.thenewmotion.ocpi
package registration

import msgs.OcpiStatusCode.GenericSuccess

import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import com.thenewmotion.ocpi.common.{ErrRespMar, SuccessRespMar}
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import msgs.Ownership.{Ours, Theirs}
import msgs.{GlobalPartyId, SuccessResp}

class RegistrationRoute(
  service: RegistrationService
)(
  implicit mat: Materializer,
  errorM: ErrRespMar,
  succOurCredsM: SuccessRespMar[Creds[Ours]],
  succUnitM: SuccessRespMar[Unit],
  theirCredsU: FromEntityUnmarshaller[Creds[Theirs]]
) extends JsonApi {

  def route(
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
