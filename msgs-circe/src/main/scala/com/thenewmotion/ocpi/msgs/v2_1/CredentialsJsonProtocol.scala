package com.thenewmotion.ocpi.msgs.v2_1

import com.thenewmotion.ocpi.msgs._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import io.circe.{Decoder, Encoder}
import CommonJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails

trait CredentialsJsonProtocol {

  implicit def credsE[O <: Ownership]: Encoder[Creds[O]] =
    Encoder.forProduct5("token", "url", "business_details", "country_code", "party_id")(c =>
      (c.token, c.url, c.businessDetails, c.globalPartyId.countryCode, c.globalPartyId.partyId)
    )

  implicit def credsD[O <: Ownership]: Decoder[Creds[O]] =
    Decoder.forProduct5("token", "url", "business_details", "country_code", "party_id") {
      (token: AuthToken[O#Opposite], url: Url, businessDetails: BusinessDetails, countryCode: String, partyId: String) =>
        Creds[O](token, url, businessDetails, GlobalPartyId(countryCode, partyId))
    }

}

object CredentialsJsonProtocol extends CredentialsJsonProtocol

