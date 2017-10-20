package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Sessions.SessionId
import io.circe.{Decoder, Encoder}

trait SessionJsonProtocol {
  implicit val sessionIdE: Encoder[SessionId] = stringEncoder(_.value)
  implicit val sessionIdD: Decoder[SessionId] = tryStringDecoder(SessionId.apply)
}

object SessionJsonProtocol extends SessionJsonProtocol
