package com.thenewmotion.ocpi.msgs
package circe.v2_1

import v2_1.Sessions.{Session, SessionId, SessionPatch, SessionStatus}
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import TokensJsonProtocol._
import CdrsJsonProtocol._
import LocationsJsonProtocol._

trait SessionJsonProtocol {
  implicit val sessionIdE: Encoder[SessionId] = stringEncoder(_.value)
  implicit val sessionIdD: Decoder[SessionId] = tryStringDecoder(SessionId.apply)

  implicit val sessionE: Encoder[Session] = deriveConfiguredEncoder
  implicit val sessionD: Decoder[Session] = deriveConfiguredDecoder

  implicit val sessionPatchE: Encoder[SessionPatch] = deriveConfiguredEncoder
  implicit val sessionPatchD: Decoder[SessionPatch] = deriveConfiguredDecoder
}

object SessionJsonProtocol extends SessionJsonProtocol
