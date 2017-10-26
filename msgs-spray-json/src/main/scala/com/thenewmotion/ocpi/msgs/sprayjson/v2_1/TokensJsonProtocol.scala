package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import v2_1.Tokens._
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{ConnectorId, EvseUid, LocationId}
import sprayjson.SimpleStringEnumSerializer
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

trait TokensJsonProtocol {

  private def deserializeLocationReferences(
    locationId: LocationId,
    evseUids: Option[Iterable[EvseUid]],
    connectorIds: Option[Iterable[ConnectorId]]
  ) = new LocationReferences(
    locationId,
    evseUids.getOrElse(Nil),
    connectorIds.getOrElse(Nil)
  )

  private implicit val tokenTypeFormat =
    new SimpleStringEnumSerializer[TokenType](TokenType).enumFormat

  private implicit val whitelistTypeFormat =
    new SimpleStringEnumSerializer[WhitelistType](WhitelistType).enumFormat

  implicit val tokenUidFmt = new JsonFormat[TokenUid] {
    override def read(json: JsValue) = json match {
      case JsString(s) => TokenUid(s)
      case _           => deserializationError("TokenUid must be a string")
    }
    override def write(obj: TokenUid) = JsString(obj.value)
  }

  implicit val authIdFmt = new JsonFormat[AuthId] {
    override def read(json: JsValue) = json match {
      case JsString(s) => AuthId(s)
      case _           => deserializationError("AuthId must be a string")
    }
    override def write(obj: AuthId) = JsString(obj.value)
  }

  implicit val tokensFormat = jsonFormat9(Token)

  implicit val tokenPatchFormat = jsonFormat9(TokenPatch)

  implicit val locationReferencesFormat = new RootJsonFormat[LocationReferences] {
    val readFormat = jsonFormat3(deserializeLocationReferences)
    val writeFormat = jsonFormat3(LocationReferences.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: LocationReferences): JsValue = writeFormat.write(obj)
  }

  private implicit val allowedFormat =
    new SimpleStringEnumSerializer[Allowed](Allowed).enumFormat

  implicit val authorizationInfoFormat = jsonFormat3(AuthorizationInfo)
}

object TokensJsonProtocol extends TokensJsonProtocol
