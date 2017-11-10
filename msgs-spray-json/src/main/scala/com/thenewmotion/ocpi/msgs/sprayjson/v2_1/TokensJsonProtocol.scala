package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import v2_1.Tokens._
import DefaultJsonProtocol._
import LocationsJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{ConnectorId, EvseUid, LocationId}
import sprayjson.SimpleStringEnumSerializer._
import spray.json.{JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}

trait TokensJsonProtocol {

  private def deserializeLocationReferences(
    locationId: LocationId,
    evseUids: Option[Iterable[EvseUid]],
    connectorIds: Option[Iterable[ConnectorId]]
  ) = LocationReferences(
    locationId,
    evseUids.getOrElse(Nil),
    connectorIds.getOrElse(Nil)
  )

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

  implicit val tokenPatchFormat = jsonFormat8(TokenPatch)

  implicit val locationReferencesFormat = new RootJsonFormat[LocationReferences] {
    val readFormat = jsonFormat3(deserializeLocationReferences)
    val writeFormat = jsonFormat3(LocationReferences.apply)
    override def read(json: JsValue) = readFormat.read(json)
    override def write(obj: LocationReferences): JsValue = writeFormat.write(obj)
  }

  implicit val authorizationInfoFormat = jsonFormat3(AuthorizationInfo)
}

object TokensJsonProtocol extends TokensJsonProtocol
