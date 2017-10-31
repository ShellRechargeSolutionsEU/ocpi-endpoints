package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import java.time.ZonedDateTime
import v2_1.Sessions.{Session, SessionId, SessionPatch, SessionStatus}
import CdrsJsonProtocol._
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import TokensJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.Cdrs.{AuthMethod, ChargingPeriod}
import com.thenewmotion.ocpi.msgs.v2_1.Locations.Location
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.AuthId
import sprayjson.SimpleStringEnumSerializer
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

trait SessionJsonProtocol {

  implicit val sessionIdFmt = new JsonFormat[SessionId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => SessionId(s)
      case _           => deserializationError("SessionId must be a string")
    }
    override def write(obj: SessionId) = JsString(obj.value)
  }

  implicit val sessionStatusFormat =
    new SimpleStringEnumSerializer[SessionStatus](SessionStatus).enumFormat

  private def deserializeSession(
    id: SessionId,
    startDatetime: ZonedDateTime,
    endDatetime: Option[ZonedDateTime],
    kwh: Int,
    authId: AuthId,
    authMethod: AuthMethod,
    location: Location,
    meterId: Option[String],
    currency: CurrencyCode,
    chargingPeriods: Option[Seq[ChargingPeriod]],
    totalCost: Option[BigDecimal],
    status: SessionStatus,
    lastUpdated: ZonedDateTime
  ): Session =
    Session(
      id,
      startDatetime,
      endDatetime,
      kwh,
      authId,
      authMethod,
      location,
      meterId,
      currency,
      chargingPeriods.getOrElse(Nil),
      totalCost,
      status,
      lastUpdated
    )

  implicit val sessionFmt = new RootJsonFormat[Session] {
    val readFormat = jsonFormat13(deserializeSession)
    val writeFormat = jsonFormat13(Session.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: Session): JsValue = writeFormat.write(obj)
  }

  implicit val sessionPatchFmt = jsonFormat13(SessionPatch.apply)
}

object SessionJsonProtocol extends SessionJsonProtocol
