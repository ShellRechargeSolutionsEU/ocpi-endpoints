package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.Errors._
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

trait HandshakeDataHandler {
  def persistClientPrefs(version: String, auth: String, creds: Credentials): CouldNotPersistPreferences \/ Unit

  def persistNewToken(auth: String, newToken: String): CouldNotPersistNewToken \/ Unit

  def persistEndpoint(version: String,  auth: String, name: String, url: Url): CouldNotPersistEndpoint \/ Unit

  def config: HandshakeConfig
}

case class HandshakeConfig (
  host: String,
  port: Int,
  partyname: String,
  namespace: String,
  externalBaseUrl: String,
  endpoint: String
  )