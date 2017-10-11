package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Sessions.SessionId
import spray.json.{JsString, JsValue, JsonFormat, deserializationError}

trait SessionJsonProtocol {
  implicit val sessionIdFmt = new JsonFormat[SessionId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => SessionId(s)
      case _ => deserializationError("SessionId must be a string")
    }
    override def write(obj: SessionId) = JsString(obj.value)
  }
}

object SessionJsonProtocol extends SessionJsonProtocol