package com.thenewmotion.ocpi
package commands

import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import cats.syntax.either._
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{Command, CommandResponse, CommandResponseType}

import scala.concurrent.{ExecutionContext, Future}

class CommandClient(
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  sucU: SuccessRespUnMar[Either[CommandResponseType, CommandResponse]]
) extends OcpiClient {

  def sendCommand[C <: Command : ToEntityMarshaller](
    commandsUri: Uri,
    auth: AuthToken[Ours],
    command: C
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[ErrorRespOr[CommandResponseType]] = {

    val commandUri = commandsUri.copy(path = commandsUri.path / command.name.name)

    singleRequest[Either[CommandResponseType, CommandResponse]](Post(commandUri, command), auth).map {
      _.bimap(err => {
        logger.error(s"Could not post command to $commandUri. Reason: $err")
        err
      }, _.data match {
        case Left(crt) => crt
        case Right(cr) => cr.result
      } )
    }

  }

}

