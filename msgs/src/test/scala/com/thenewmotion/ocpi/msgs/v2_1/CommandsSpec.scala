package com.thenewmotion.ocpi.msgs.v2_1

import java.util.UUID

import com.thenewmotion.ocpi.msgs.Url
import com.thenewmotion.ocpi.msgs.v2_1.Commands.CommandName
import org.specs2.mutable.Specification

class CommandsSpec extends Specification {

  "Commands" should {
    "Construct a callback url" >> {
      val uuid = UUID.randomUUID()
      Commands.callbackUrl(Url("http://blah.com"), CommandName.StartSession, uuid) mustEqual
        Url(s"http://blah.com/START_SESSION/$uuid")
    }
  }

}
