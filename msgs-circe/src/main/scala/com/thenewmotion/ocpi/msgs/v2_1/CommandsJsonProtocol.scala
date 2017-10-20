package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import com.thenewmotion.ocpi.msgs.v2_1.Commands.{Command, CommandResponse, CommandResponseType}
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import TokensJsonProtocol._
import LocationsJsonProtocol._
import SessionJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.Commands.Command.UnlockConnector
import io.circe.generic.extras.semiauto._

trait CommandsJsonProtocol {

  private implicit val commandResponseTypeE: Encoder[CommandResponseType] =
    SimpleStringEnumSerializer.encoder(CommandResponseType)

  private implicit val commandResponseTypeD: Decoder[CommandResponseType] =
    SimpleStringEnumSerializer.decoder(CommandResponseType)

  implicit val commandResponseE: Encoder[CommandResponse] = deriveEncoder
  implicit val commandResponseD: Decoder[CommandResponse] = deriveDecoder

  implicit val reserveNowE: Encoder[Command.ReserveNow] = deriveEncoder
  implicit val reserveNowD: Decoder[Command.ReserveNow] = deriveDecoder

  implicit val startSessionE: Encoder[Command.StartSession] = deriveEncoder
  implicit val startSessionD: Decoder[Command.StartSession] = deriveDecoder

  implicit val stopSessionE: Encoder[Command.StopSession] = deriveEncoder
  implicit val stopSessionD: Decoder[Command.StopSession] = deriveDecoder

  implicit val unlockConnectorE: Encoder[UnlockConnector] = deriveEncoder
  implicit val unlockConnectorD: Decoder[UnlockConnector] = deriveDecoder
}

object CommandsJsonProtocol extends CommandsJsonProtocol
