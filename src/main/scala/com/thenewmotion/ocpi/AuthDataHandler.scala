package com.thenewmotion.ocpi

trait TopLevelRouteDataHandler {
  def namespace: String
}

trait AuthDataHandler {


  def authenticateApiUser(token: String): Option[ApiUser]
}

case class ApiUser(
  id: String,
  token: String
  )