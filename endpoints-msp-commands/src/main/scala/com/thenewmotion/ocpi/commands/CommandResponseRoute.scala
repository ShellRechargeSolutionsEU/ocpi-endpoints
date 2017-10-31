package com.thenewmotion.ocpi
package commands

import java.util.UUID
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import msgs.v2_1.Commands.{CommandResponse, CommandResponseType}
import common._
import msgs._
import scala.concurrent.Future

object CommandResponseRoute {
  def apply(
    callback: (GlobalPartyId, UUID, CommandResponseType) => Future[Option[SuccessResp[Unit]]]
  )(
    implicit errorM: ErrRespMar,
    succM: SuccessRespMar[Unit],
    reqUm: FromEntityUnmarshaller[CommandResponse]
  ) = new CommandResponseRoute(callback)
}

class CommandResponseRoute private[ocpi](
  callback: (GlobalPartyId, UUID, CommandResponseType) => Future[Option[SuccessResp[Unit]]]
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
