package com.thenewmotion.ocpi
package commands

import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.effect.{ContextShift, IO}
import cats.syntax.either._
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{Command, CommandResponse, CommandResponseType}
import com.thenewmotion.ocpi.msgs.{AuthToken, SuccessResp}
import scala.concurrent.ExecutionContext

class CommandClient(
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  sucU: FromEntityUnmarshaller[Either[SuccessResp[CommandResponseType], SuccessResp[CommandResponse]]]
) extends OcpiClient {

  def sendCommand[C <: Command : ToEntityMarshaller](
    commandsUri: Uri,
    auth: AuthToken[Ours],
    command: C
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[CommandResponseType]] = {

    val commandUri = commandsUri.copy(path = commandsUri.path ?/ command.name.name)

    singleRequestRawT[Either[SuccessResp[CommandResponseType], SuccessResp[CommandResponse]]](Post(commandUri, command), auth).map {
      _.bimap(err => {
        logger.error(s"Could not post command to $commandUri. Reason: $err")
        err
      }, {
        case Left(crt) => crt.data
        case Right(cr) => cr.data.result
      })
    }

  }

}

