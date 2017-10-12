package com.thenewmotion.ocpi.msgs.v2_1

import java.util.UUID

import com.thenewmotion.ocpi.msgs.Url
import org.specs2.mutable.Specification

class CommandsSpec extends Specification {

  "Commands" should {
    "Construct a callback url" >> {
      val uuid = UUID.randomUUID()
      Commands.callbackUrl(Url("http://blah.com/commands"), uuid) mustEqual
        Url(s"http://blah.com/commands/$uuid")
    }
  }

}
