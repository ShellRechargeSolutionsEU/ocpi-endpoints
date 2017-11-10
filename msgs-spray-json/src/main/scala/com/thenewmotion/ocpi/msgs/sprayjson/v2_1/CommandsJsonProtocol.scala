package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import v2_1.Commands.{Command, CommandResponse, CommandResponseType}
import DefaultJsonProtocol._
import SessionJsonProtocol._
import TokensJsonProtocol._
import LocationsJsonProtocol._
import sprayjson.SimpleStringEnumSerializer._

trait CommandsJsonProtocol {

  implicit val commandResponse = jsonFormat1(CommandResponse)

  implicit val reserveNowF = jsonFormat6(Command.ReserveNow.apply)
  implicit val startSessionF = jsonFormat4(Command.StartSession.apply)
  implicit val stopSessionF = jsonFormat2(Command.StopSession.apply)
  implicit val unlockConnectorF = jsonFormat4(Command.UnlockConnector.apply)
}

object CommandsJsonProtocol extends CommandsJsonProtocol
