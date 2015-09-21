package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{BusinessDetails => OcpiBusinessDetails, Url}
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.Creds
import org.joda.time.format.ISODateTimeFormat
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz._

class HandshakeServiceSpec extends Specification  with Mockito {

  "HandshakeService should" should {
    "accept client credentials" in new HandshakeTestScope {

    }
  }

  trait HandshakeTestScope extends Scope {


  }
}
