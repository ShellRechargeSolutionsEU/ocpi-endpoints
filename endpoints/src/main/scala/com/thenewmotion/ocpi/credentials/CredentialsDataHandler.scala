package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.credentials.CredentialsErrors.RegistrationError
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.Url
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
  def persistClientPrefs(version: String, auth: String, creds: Credentials): RegistrationError \/ Unit

  def persistNewToken(auth: String, newToken: String): RegistrationError \/ Unit

  def persistEndpoint(version: String,  auth: String, name: String, url: Url): RegistrationError \/ Unit

  def config: CredentialsConfig
}

case class CredentialsConfig (
  host: String,
  port: Int,
  partyname: String,
  namespace: String,
  externalBaseUrl: String,
  endpoint: String
  )