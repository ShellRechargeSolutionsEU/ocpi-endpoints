package com.thenewmotion.ocpi.msgs.v2_1

object Sessions {

  trait SessionId extends Any { def value: String }
  object SessionId {
    private case class SessionIdImpl(value: String) extends AnyVal with SessionId {
      override def toString: String = value
    }

    def apply(value: String): SessionId = {
      require(value.length <= 36, "Session Id must be 36 characters or less")
      SessionIdImpl(value)
    }

    def unapply(tokId: SessionId): Option[String] =
      Some(tokId.value)
  }

}
