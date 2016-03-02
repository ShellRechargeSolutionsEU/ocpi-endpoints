package com.thenewmotion.ocpi.handshake

sealed trait HandshakeError {
  val reason: String
}

object HandshakeError{
  case object VersionsRetrievalFailed extends HandshakeError{
    override val reason: String = "Failed versions retrieval."
  }
  case object VersionDetailsRetrievalFailed extends HandshakeError{
    override val reason: String = "Failed version details retrieval."
  }
  case object SendingCredentialsFailed extends HandshakeError{
    override val reason: String = "Failed sending the credentials to connect to us."
  }
  case class SelectedVersionNotHostedByUs(version: String) extends HandshakeError{
    override val reason: String = s"The selected version: $version, is not supported by our systems."
  }
  case object CouldNotFindMutualVersion extends HandshakeError{
    override val reason: String = "Could not find mutual version."
  }
  case class SelectedVersionNotHostedByThem(version: String) extends HandshakeError{
    override val reason: String = s"Selected version: $version, not supported by the requester party systems."
  }
//  case object NoCredentialsEndpoint extends HandshakeError{
//    override val reason: String = "Credentials endpoint details required but not found."   }
  case class UnknownEndpointType(endpointType: String) extends HandshakeError{
    override val reason: String = s"Unknown endpoint type: $endpointType"
}
  case object CouldNotPersistCredsForUs extends HandshakeError{
    override val reason: String = "Could not persist credentials sent to us."
  }
  case object CouldNotPersistNewCredsForUs extends HandshakeError{
    override val reason: String = "Could not persist the new credentials sent to us."
  }
  case class CouldNotPersistNewToken(newToken: String) extends HandshakeError{
    override val reason: String = s"Could not persist the new token: $newToken."
  }
  case class CouldNotPersistNewEndpoint(endpoint: String) extends HandshakeError{
    override val reason: String = s"Could not persist new endpoint: $endpoint."
  }
  case object CouldNotUpdateEndpoints extends HandshakeError{
    override val reason: String = "Could not update registered endpoints."
  }
  case class CouldNotPersistNewParty(partyId: String) extends HandshakeError{
    override val reason: String = s"Could not persist new party: $partyId."
  }
  case class AlreadyExistingParty(partyId: String, country: String, version: String) extends HandshakeError{
    override val reason: String = s"Already existing partyId: '$partyId' for country: '$country' and version: '$version'."
  }
  case class UnknownPartyToken(token: String) extends HandshakeError{
    override val reason: String = s"Unknown party token: '$token'."
  }
  case object WaitingForRegistrationRequest extends HandshakeError{
    override val reason: String = "Still waiting for registration request."
  }
}