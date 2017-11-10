package com.thenewmotion.ocpi.msgs

sealed trait OcpiStatusCode {
  def code: Int
}

object OcpiStatusCode {
  def apply(code: Int): OcpiStatusCode = code match {
    case x if x >= 1000 && x <= 1999 => SuccessCode(code)
    case x if x >= 2000 && x <= 2999 => ClientErrorCode(code)
    case x if x >= 3000 && x <= 3999 => ServerErrorCode(code)
    case x => throw new RuntimeException(s"$x is not a valid OCPI Status Code")
  }

  sealed abstract case class SuccessCode private[OcpiStatusCode](code: Int) extends OcpiStatusCode
  object SuccessCode {
    private[OcpiStatusCode] def apply(code: Int) = new SuccessCode(code) {}
  }

  sealed trait ErrorCode extends OcpiStatusCode
  sealed abstract case class ClientErrorCode private[OcpiStatusCode](code: Int) extends ErrorCode
  object ClientErrorCode {
    private[OcpiStatusCode] def apply(code: Int) = new ClientErrorCode(code) {}
  }

  sealed abstract case class ServerErrorCode private[OcpiStatusCode](code: Int) extends ErrorCode
  object ServerErrorCode {
    private[OcpiStatusCode] def apply(code: Int) = new ServerErrorCode(code) {}
  }

  val GenericSuccess = SuccessCode(1000)
  val GenericClientFailure = ClientErrorCode(2000)
  val InvalidOrMissingParameters = ClientErrorCode(2001)
  val NotEnoughInformation = ClientErrorCode(2002)
  val UnknownLocation = ClientErrorCode(2003)
  val AuthenticationFailed = ClientErrorCode(2010)
  val MissingHeader = ClientErrorCode(2011)
  val PartyAlreadyRegistered = ClientErrorCode(2012) // When POSTing
  val RegistrationNotCompletedYetByParty = ClientErrorCode(2013) // When PUTing or GETing even
  val AuthorizationFailed = ClientErrorCode(2014)
  val ClientWasNotRegistered = ClientErrorCode(2015)

  val GenericServerFailure = ServerErrorCode(3000)
  val UnableToUseApi = ServerErrorCode(3001)
  val UnsupportedVersion = ServerErrorCode(3002)
  val MissingExpectedEndpoints = ServerErrorCode(3003) //TODO: TNM-2013
  val UnknownEndpointType = ServerErrorCode(3010)
}
