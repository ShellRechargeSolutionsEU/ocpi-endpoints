package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds => OcpiCredentials}
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails}
import com.thenewmotion.ocpi.{ListError, ApiUser, CreateError}

import scalaz.\/


case class BusinessDetails(
  name: String,
  logo: Option[String] = None,
  website: Option[String] = None
  )

case class Credentials (
  token: String,
  versions_url:  String,
  business_details: BusinessDetails

)

object Credentials {
  def fromOcpiClass(crds: OcpiCredentials): Credentials = Credentials(
    token = crds.token,
    versions_url = crds.url,
    business_details = BusinessDetails(
      name = crds.business_details.name,
      logo = Some(crds.business_details.logo),
      website = Some(crds.business_details.website)
    )
  )
}

trait CredentialsDataHandler {

  def registerVersionsEndpoint(version: String, auth: String, creds: Credentials): CreateError \/ Unit

  def retrieveCredentials: ListError \/ Credentials

}
