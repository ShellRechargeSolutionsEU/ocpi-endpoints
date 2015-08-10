package com.thenewmotion.ocpi

trait TopLevelRouteDataHanlder {
  def namespace: String
}

trait AuthDataHandler {


  def apiuser(token: String): Option[ApiUser]
}

case class ApiUser(
  id: String
  )