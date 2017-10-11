package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.SimpleStringEnumSerializer
import Commands._
import DefaultJsonProtocol._
import TokensJsonProtocol._
import LocationsJsonProtocol._
import SessionJsonProtocol._

trait CommandsJsonProtocol {

  private implicit val commandResponseTypeFormat =
    new SimpleStringEnumSerializer(CommandResponseType).enumFormat

  implicit val commandResponse = jsonFormat1(CommandResponse)

  implicit val reserveNowF = jsonFormat6(Command.ReserveNow.apply)
  implicit val startSessionF = jsonFormat4(Command.StartSession.apply)
  implicit val stopSessionF = jsonFormat2(Command.StopSession.apply)
  implicit val unlockConnectorF = jsonFormat4(Command.UnlockConnector.apply)
}

object CommandsJsonProtocol extends CommandsJsonProtocol
