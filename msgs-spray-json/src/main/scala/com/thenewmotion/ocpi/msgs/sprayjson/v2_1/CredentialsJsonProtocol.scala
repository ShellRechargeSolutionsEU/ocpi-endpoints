package com.thenewmotion.ocpi.msgs
package sprayjson.v2_1

import v2_1.CommonTypes.BusinessDetails
import v2_1.Credentials.Creds
import DefaultJsonProtocol._
import spray.json.{JsValue, RootJsonFormat}

trait CredentialsJsonProtocol {

  implicit def credentialsFormat[O <: Ownership] = new RootJsonFormat[Creds[O]] {

    private case class InternalCreds(
      token: AuthToken[O#Opposite],
      url: Url,
      businessDetails: BusinessDetails,
      countryCode: String,
      partyId: String
    )

    private implicit val intF = jsonFormat5(InternalCreds)

    override def write(obj: Creds[O]) =
      intF.write(
        InternalCreds(
          obj.token,
          obj.url,
          obj.businessDetails,
          obj.globalPartyId.countryCode,
          obj.globalPartyId.partyId
        )
      )

    override def read(json: JsValue) = {
      intF.read(json) match {
        case InternalCreds(t, u, bd, c, p) =>
          Creds[O](t, u, bd, GlobalPartyId(c, p))
      }
    }
  }
}

object CredentialsJsonProtocol extends CredentialsJsonProtocol
