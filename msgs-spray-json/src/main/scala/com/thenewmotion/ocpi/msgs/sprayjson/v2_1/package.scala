package com.thenewmotion.ocpi.msgs.sprayjson

import com.thenewmotion.ocpi.msgs.v2_1.Id
import spray.json.{JsValue, JsonFormat}

package object v2_1 {

  implicit def idFmt[T : JsonFormat]: JsonFormat[Id[T]] = new JsonFormat[Id[T]] {
    override def read(json: JsValue) = implicitly[JsonFormat[T]].read(json)
    override def write(obj: Id[T]) = implicitly[JsonFormat[T]].write(obj)
  }

  object protocol
    extends CdrsJsonProtocol
      with CommandsJsonProtocol
      with CredentialsJsonProtocol
      with DefaultJsonProtocol
      with LocationsJsonProtocol
      with SessionJsonProtocol
      with TariffsJsonProtocol
      with TokensJsonProtocol
      with VersionsJsonProtocol
}
