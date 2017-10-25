package com.thenewmotion.ocpi.msgs
package circe.v2_1

import circe.SimpleStringEnumSerializer
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

  implicit val sessionStatusE: Encoder[SessionStatus] = SimpleStringEnumSerializer.encoder(SessionStatus)
  implicit val sessionStatusD: Decoder[SessionStatus] = SimpleStringEnumSerializer.decoder(SessionStatus)

  implicit val sessionE: Encoder[Session] = deriveEncoder
  implicit val sessionD: Decoder[Session] = deriveDecoder

  implicit val sessionPatchE: Encoder[SessionPatch] = deriveEncoder
  implicit val sessionPatchD: Decoder[SessionPatch] = deriveDecoder

}

object SessionJsonProtocol extends SessionJsonProtocol
