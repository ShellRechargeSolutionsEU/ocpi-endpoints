package com.thenewmotion.ocpi
package commands

import java.util.UUID

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import msgs.v2_1.Commands.{CommandResponse, CommandResponseType}
import common.{EitherUnmarshalling, OcpiDirectives, OcpiRejectionHandler}
import msgs._

import scala.concurrent.Future

class CommandResponseRoute(
  callback: (GlobalPartyId, UUID, CommandResponseType) => Future[Option[SuccessResp[Unit]]]
)(
  implicit errorM: ToEntityMarshaller[ErrorResp],
  succM: ToEntityMarshaller[SuccessResp[Unit]],
  reqUm: FromEntityUnmarshaller[CommandResponse]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  def route(
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
