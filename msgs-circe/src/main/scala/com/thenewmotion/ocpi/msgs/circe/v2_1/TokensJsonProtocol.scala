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

  implicit val tokenE: Encoder[Token] = deriveEncoder
  implicit val tokenD: Decoder[Token] = deriveDecoder

  implicit val tokenPatchE: Encoder[TokenPatch] = deriveEncoder
  implicit val tokenPatchD: Decoder[TokenPatch] = deriveDecoder

  implicit val locationReferencesE: Encoder[LocationReferences] = deriveEncoder
  implicit val locationReferencesD: Decoder[LocationReferences] = deriveDecoder

  implicit val authorizationInfoE: Encoder[AuthorizationInfo] = deriveEncoder
  implicit val authorizationInfoD: Decoder[AuthorizationInfo] = deriveDecoder
}

object TokensJsonProtocol extends TokensJsonProtocol
