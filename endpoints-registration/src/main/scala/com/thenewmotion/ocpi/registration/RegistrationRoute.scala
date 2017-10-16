package com.thenewmotion.ocpi
package registration

import msgs.OcpiStatusCode.GenericSuccess

import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import akka.stream.Materializer
import msgs.Ownership.Theirs
import msgs.{GlobalPartyId, SuccessResp}

class RegistrationRoute(service: RegistrationService)(implicit mat: Materializer) extends JsonApi {
  import msgs.v2_1.Credentials._
  import msgs.v2_1.DefaultJsonProtocol._
  import msgs.v2_1.CredentialsJsonProtocol._

  def route(accessedVersion: Version, user: GlobalPartyId)(implicit ec: ExecutionContext) = {
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
