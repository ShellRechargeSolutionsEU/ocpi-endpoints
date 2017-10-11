package com.thenewmotion.ocpi
package commands

import java.util.UUID

import akka.http.scaladsl.server.Route
import msgs.OcpiStatusCode.GenericSuccess
import msgs.v2_1.Commands.{CommandName, CommandResponse, CommandResponseType}
import common.{EitherUnmarshalling, OcpiDirectives, OcpiRejectionHandler}
import msgs._

import scala.concurrent.{ExecutionContext, Future}

class CommandResponseRoute(
  callback: (GlobalPartyId, CommandName, UUID, CommandResponseType) => Future[Unit]
) extends JsonApi with EitherUnmarshalling with OcpiDirectives {

  import msgs.v2_1.DefaultJsonProtocol._
  import msgs.v2_1.CommandsJsonProtocol._

  def route(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext): Route =
    handleRejections(OcpiRejectionHandler.Default)(routeWithoutRh(apiUser))

  private val CommandNameSegment = Segment.flatMap(x => CommandName.values.find(_.name == x))

  private[commands] def routeWithoutRh(apiUser: GlobalPartyId)(implicit executionContext: ExecutionContext) =
    authPathPrefixGlobalPartyIdEquality(apiUser) {
      (path(CommandNameSegment) & path(JavaUUID)) { (commandName, commandId) =>
        pathEndOrSingleSlash {
          post {
            entity(as[CommandResponse]) { response =>
              complete {
                callback(apiUser, commandName, commandId, response.result).map(_ => SuccessResp(GenericSuccess))
              }
            }
          }
        }
      }
    }

}
