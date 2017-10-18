package com.thenewmotion.ocpi
package commands

import java.util.UUID

import akka.http.scaladsl.server.Route
import msgs.v2_1.Commands.{CommandResponse, CommandResponseType}
import common.{EitherUnmarshalling, OcpiDirectives, OcpiRejectionHandler}
import msgs._

import scala.concurrent.Future

class CommandResponseRoute(
  callback: (GlobalPartyId, UUID, CommandResponseType) => Future[Option[SuccessResp[Unit]]]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  import msgs.v2_1.DefaultJsonProtocol._
  import msgs.v2_1.CommandsJsonProtocol._

  def route(apiUser: GlobalPartyId): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private[commands] def routeWithoutRh(apiUser: GlobalPartyId) =
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
