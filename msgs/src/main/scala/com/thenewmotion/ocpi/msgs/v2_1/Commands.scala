package com.thenewmotion.ocpi.msgs.v2_1

import java.time.ZonedDateTime

import com.thenewmotion.ocpi.msgs.Url
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{ConnectorId, EvseUid, LocationId}
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.SessionId
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.Token
import com.thenewmotion.ocpi.{Enumerable, Nameable}

object Commands {
  sealed trait CommandType extends Nameable
  object CommandType extends Enumerable[CommandType] {
    case object ReserveNow extends CommandType {val name = "RESERVE_NOW"}
    case object StartSession extends CommandType {val name = "START_SESSION"}
    case object StopSession extends CommandType {val name = "STOP_SESSION"}
    case object UnlockConnector extends CommandType {val name = "UNLOCK_CONNECTOR"}
    val values = Seq(ReserveNow, StartSession, StopSession, UnlockConnector)
  }

  case class ReserveNow(
    responseUrl: Url,
    token: Token,
    expiryDate: ZonedDateTime,
    reservationId: Int,
    locationId: LocationId,
    evseUid: EvseUid
  )

  case class StartSession(
    responseUrl: Url,
    token: Token,
    locationId: LocationId,
    evseUid: EvseUid
  )

  case class StopSession(
    responseUrl: Url,
    sessionId: SessionId
  )

  case class UnlockConnector(
    responseUrl: Url,
    locationId: LocationId,
    evseUid: EvseUid,
    connectorId: ConnectorId
  )
}
