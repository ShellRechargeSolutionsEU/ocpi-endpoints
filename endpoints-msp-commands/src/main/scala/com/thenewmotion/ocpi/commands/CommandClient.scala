package com.thenewmotion.ocpi
package commands

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import common.OcpiClient
import msgs.AuthToken
import msgs.Ownership.Ours
import msgs.v2_1.Commands.{Command, CommandResponse}
import cats.syntax.either._
import spray.json.RootJsonWriter

import scala.concurrent.{ExecutionContext, Future}

class CommandClient(implicit http: HttpExt) extends OcpiClient {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import msgs.v2_1.DefaultJsonProtocol._
  import msgs.v2_1.CommandsJsonProtocol._

  def sendCommand[C <: Command : RootJsonWriter](
    commandsUri: Uri,
    auth: AuthToken[Ours],
    command: C
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorRespOr[CommandResponse]] = {

    val commandUri = commandsUri.copy(path = commandsUri.path / command.name.name)

    singleRequest[CommandResponse](Post(commandUri, command), auth).map {
      _.bimap(err => {
        logger.error(s"Could not post command to $commandUri. Reason: $err")
        err
      }, _.data)
    }

  }

}

