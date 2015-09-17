package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds => OcpiCredentials}

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
      logo = crds.business_details.logo,
      website = crds.business_details.website
    )
  )
}

trait CredentialsDataHandler {

  def registerVersionsEndpoint(version: String, auth: String, creds: Credentials): RegistrationError \/ OcpiCredentials

}
