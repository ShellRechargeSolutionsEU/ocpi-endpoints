package com.thenewmotion.ocpi.msgs
package circe.v2_1

import Versions._
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._

trait VersionsJsonProtocol {

  implicit val versionNumberE: Encoder[VersionNumber] = stringEncoder(_.toString)
  implicit val versionNumberD: Decoder[VersionNumber] = tryStringDecoder(VersionNumber.apply)

  implicit val versionE: Encoder[Version] = deriveConfiguredEncoder
  implicit val versionD: Decoder[Version] = deriveConfiguredDecoder

  implicit val endpointE: Encoder[Endpoint] = deriveConfiguredEncoder
  implicit val endpointD: Decoder[Endpoint] = deriveConfiguredDecoder

  implicit val versionDetailsE: Encoder[VersionDetails] = deriveConfiguredEncoder
  implicit val versionDetailsD: Decoder[VersionDetails] = deriveConfiguredDecoder
}

object VersionsJsonProtocol extends VersionsJsonProtocol
