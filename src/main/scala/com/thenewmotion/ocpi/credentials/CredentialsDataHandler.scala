package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.{ApiUser, CreateError}

import scalaz.\/

trait CredentialsDataHandler {

  def registerToken(apiUser: ApiUser, token: String): CreateError \/ Unit
}
