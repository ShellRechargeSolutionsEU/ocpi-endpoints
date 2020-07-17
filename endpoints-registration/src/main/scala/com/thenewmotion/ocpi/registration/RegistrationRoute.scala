package com.thenewmotion.ocpi
package registration

import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import _root_.akka.stream.Materializer
import cats.effect.{ContextShift, Effect, IO}
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericSuccess
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.Versions.VersionNumber
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, SuccessResp}
import scala.concurrent.ExecutionContext

object RegistrationRoute {
  def apply[F[_]: Effect: HktMarshallable](
    service: RegistrationService
  )(
    implicit mat: Materializer,
    errorM: ErrRespMar,
    succOurCredsM: SuccessRespMar[Creds[Ours]],
    succUnitM: SuccessRespMar[Unit],
    theirCredsU: FromEntityUnmarshaller[Creds[Theirs]],
    cs: ContextShift[IO]
  ): RegistrationRoute[F] = new RegistrationRoute(service)
}

class RegistrationRoute[F[_]: Effect: HktMarshallable] private[ocpi](
  service: RegistrationService
)(
  implicit mat: Materializer,
  errorM: ErrRespMar,
  succOurCredsM: SuccessRespMar[Creds[Ours]],
  succUnitM: SuccessRespMar[Unit],
  theirCredsU: FromEntityUnmarshaller[Creds[Theirs]],
  cs: ContextShift[IO]
) extends OcpiDirectives {

  import ErrorMarshalling._
  import HktMarshallableInstances._
  import HktMarshallableSyntax._

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
