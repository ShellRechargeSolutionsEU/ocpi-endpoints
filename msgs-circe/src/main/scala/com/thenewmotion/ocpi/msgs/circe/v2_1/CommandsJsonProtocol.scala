package com.thenewmotion.ocpi.msgs
package circe.v2_1

import circe.SimpleStringEnumSerializer._
import v2_1.Commands.Command.UnlockConnector
import v2_1.Commands.{Command, CommandResponse}
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import TokensJsonProtocol._
import LocationsJsonProtocol._
import SessionJsonProtocol._

trait CommandsJsonProtocol {
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
