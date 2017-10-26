package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import v2_1.Sessions.{Session, SessionId, SessionPatch, SessionStatus}
import CdrsJsonProtocol._
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import TokensJsonProtocol._
import sprayjson.SimpleStringEnumSerializer
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

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

  implicit val sessionFmt = jsonFormat13(Session.apply)
  implicit val sessionPatchFmt = jsonFormat13(SessionPatch.apply)
}

object SessionJsonProtocol extends SessionJsonProtocol
