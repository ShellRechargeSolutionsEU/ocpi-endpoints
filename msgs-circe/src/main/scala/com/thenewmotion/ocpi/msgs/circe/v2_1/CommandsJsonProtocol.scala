package com.thenewmotion.ocpi.msgs
package circe.v2_1

import v2_1.Commands.Command.UnlockConnector
import v2_1.Commands.{Command, CommandResponse}
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import TokensJsonProtocol._
import LocationsJsonProtocol._
import SessionJsonProtocol._

trait CommandsJsonProtocol {
  implicit val commandResponseE: Encoder[CommandResponse] = deriveConfiguredEncoder
  implicit val commandResponseD: Decoder[CommandResponse] = deriveConfiguredDecoder

  implicit val reserveNowE: Encoder[Command.ReserveNow] = deriveConfiguredEncoder
  implicit val reserveNowD: Decoder[Command.ReserveNow] = deriveConfiguredDecoder

  implicit val startSessionE: Encoder[Command.StartSession] = deriveConfiguredEncoder
  implicit val startSessionD: Decoder[Command.StartSession] = deriveConfiguredDecoder

  implicit val stopSessionE: Encoder[Command.StopSession] = deriveConfiguredEncoder
  implicit val stopSessionD: Decoder[Command.StopSession] = deriveConfiguredDecoder

  implicit val unlockConnectorE: Encoder[UnlockConnector] = deriveConfiguredEncoder
  implicit val unlockConnectorD: Decoder[UnlockConnector] = deriveConfiguredDecoder
}

object CommandsJsonProtocol extends CommandsJsonProtocol
