package com.thenewmotion.ocpi
package commands

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import common.{ErrUnMar, OcpiClient}
import msgs.{AuthToken, SuccessResp}
import msgs.Ownership.Ours
import msgs.v2_1.Commands.{Command, CommandResponse}
import cats.syntax.either._

import scala.concurrent.{ExecutionContext, Future}

class CommandClient(
  implicit http: HttpExt,
  errorU: ErrUnMar,
  sucU: FromEntityUnmarshaller[SuccessResp[CommandResponse]]
) extends OcpiClient {

  def sendCommand[C <: Command : ToEntityMarshaller](
    commandsUri: Uri,
    auth: AuthToken[Ours],
    command: C
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[ErrorRespOr[CommandResponse]] = {

    val commandUri = commandsUri.copy(path = commandsUri.path / command.name.name)

    singleRequest[CommandResponse](Post(commandUri, command), auth).map {
      _.bimap(err => {
        logger.error(s"Could not post command to $commandUri. Reason: $err")
        err
      }, _.data)
    }

  }

}

