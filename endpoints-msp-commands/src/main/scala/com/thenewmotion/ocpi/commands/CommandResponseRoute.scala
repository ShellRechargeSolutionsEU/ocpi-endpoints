package com.thenewmotion.ocpi
package commands

import java.util.UUID

import akka.http.scaladsl.server.Route
import msgs.OcpiStatusCode.GenericSuccess
import msgs.v2_1.Commands.{CommandResponse, CommandResponseType}
import common.{EitherUnmarshalling, OcpiDirectives, OcpiRejectionHandler}
import msgs._

import scala.concurrent.{ExecutionContext, Future}

class CommandResponseRoute(
  callback: (GlobalPartyId, UUID, CommandResponseType) => Future[Unit]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  import msgs.v2_1.DefaultJsonProtocol._
  import msgs.v2_1.CommandsJsonProtocol._

  def route(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private[commands] def routeWithoutRh(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext) =
    (pathPrefix(JavaUUID) & pathEndOrSingleSlash) { commandId =>
      post {
        entity(as[CommandResponse]) { response =>
          complete {
            callback(apiUser, commandId, response.result).map(_ => SuccessResp(GenericSuccess))
          }
        }
      }
    }

}
