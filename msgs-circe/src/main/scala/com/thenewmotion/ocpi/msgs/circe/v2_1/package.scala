package com.thenewmotion.ocpi.msgs.circe

import io.circe.generic.extras.Configuration
import io.circe.{Decoder, Encoder, Json}
import scala.util.Try

package object v2_1 {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames.withDefaults

  def stringEncoder[T](enc: T => String): Encoder[T] =
    Encoder.encodeString.contramap[T](enc)

  def tryStringDecoder[T](dec: String => T): Decoder[T] =
    Decoder.decodeString.flatMap { str =>
      Decoder.instanceTry { _ =>
        Try(dec(str))
      }
    }

  def decodeSeqTolerantly[A: Decoder]: Decoder[Seq[A]] =
    Decoder.decodeList(Decoder[A].either(Decoder[Json])).map(
      _.flatMap(_.left.toOption)
    )

  object protocol
    extends CdrsJsonProtocol
      with CommandsJsonProtocol
      with CredentialsJsonProtocol
      with CommonJsonProtocol
      with LocationsJsonProtocol
      with SessionJsonProtocol
      with TariffsJsonProtocol
      with TokensJsonProtocol
      with VersionsJsonProtocol {
    override def strict: Boolean = true
  }
}
