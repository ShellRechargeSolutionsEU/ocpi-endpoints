package com.thenewmotion.ocpi.msgs.sprayjson

package object v2_1 {
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
