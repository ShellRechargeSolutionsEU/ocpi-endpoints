package com.thenewmotion.ocpi.msgs.circe.v2_1

import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import LocationsJsonProtocol._
import CommonJsonProtocol._

trait TokensJsonProtocol {
  implicit val tokenUidE: Encoder[TokenUid] = stringEncoder(_.value)
  implicit val tokenUidD: Decoder[TokenUid] = tryStringDecoder(TokenUid.apply)

  implicit val authIdE: Encoder[AuthId] = stringEncoder(_.value)
  implicit val authIdD: Decoder[AuthId] = tryStringDecoder(AuthId.apply)

  implicit val tokenE: Encoder[Token] = deriveConfiguredEncoder
  implicit val tokenD: Decoder[Token] = deriveConfiguredDecoder

  implicit val tokenPatchE: Encoder[TokenPatch] = deriveConfiguredEncoder
  implicit val tokenPatchD: Decoder[TokenPatch] = deriveConfiguredDecoder

  implicit val locationReferencesE: Encoder[LocationReferences] = deriveConfiguredEncoder
  implicit val locationReferencesD: Decoder[LocationReferences] = deriveConfiguredDecoder

  implicit val authorizationInfoE: Encoder[AuthorizationInfo] = deriveConfiguredEncoder
  implicit val authorizationInfoD: Decoder[AuthorizationInfo] = deriveConfiguredDecoder
}

object TokensJsonProtocol extends TokensJsonProtocol
