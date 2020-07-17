package com.thenewmotion.ocpi
package commands

import java.util.UUID
import _root_.akka.http.scaladsl.server.Route
import _root_.akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.Functor
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{CommandResponse, CommandResponseType}

object CommandResponseRoute {
  def apply[F[_]: Functor: HktMarshallable](
    callback: (GlobalPartyId, UUID, CommandResponseType) => F[Option[SuccessResp[Unit]]]
  )(
    implicit errorM: ErrRespMar,
    succM: SuccessRespMar[Unit],
    reqUm: FromEntityUnmarshaller[CommandResponse]
  ) = new CommandResponseRoute(callback)
}

class CommandResponseRoute[F[_]: Functor: HktMarshallable] private[ocpi](
  callback: (GlobalPartyId, UUID, CommandResponseType) => F[Option[SuccessResp[Unit]]]
)(
  implicit errorM: ErrRespMar,
  succM: SuccessRespMar[Unit],
  reqUm: FromEntityUnmarshaller[CommandResponse]
) extends EitherUnmarshalling
    with OcpiDirectives {

  def apply(
    apiUser: GlobalPartyId
  ): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  import HktMarshallableSyntax._

  private[commands] def routeWithoutRh(
    apiUser: GlobalPartyId
  ) =
    (pathPrefix(JavaUUID) & pathEndOrSingleSlash) { commandId =>
      post {
        entity(as[CommandResponse]) { response =>
          rejectEmptyResponse {
            complete {
              callback(apiUser, commandId, response.result)
            }
          }
        }
      }
    }

}
