package com.thenewmotion.ocpi
package registration

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import cats.effect.IO
import com.thenewmotion.ocpi.common.IOMatchersExt
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.BusinessDetails
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{AuthToken, GlobalPartyId, Url, Versions}
import com.thenewmotion.ocpi.registration.RegistrationError.{SendingCredentialsFailed, UpdatingCredentialsFailed, VersionDetailsRetrievalFailed, VersionsRetrievalFailed}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.EitherMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.specification.core.Env
import scala.concurrent.{ExecutionContext, Future}

class RegistrationClientSpec(environment: Env)
  extends Specification
  with Mockito
  with IOMatchersExt
  with EitherMatchers {

  implicit val ee: ExecutionEnv = environment.executionEnv
  implicit val ec: ExecutionContext = environment.executionContext
  implicit val sys: ActorSystem = ActorSystem()

  val uri = Uri("http://localhost/nothingHere")
  val ourToken = AuthToken[Ours]("token")
  val creds = Creds[Ours](
    AuthToken[Theirs]("token"),
    Url("http://localhost/norHere"),
    BusinessDetails("someOne", None, None),
    GlobalPartyId("party")
  )

  "Registration client recovers errors when" >> {
    "getting their versions" >> new TestScope {
      client.getTheirVersions(uri, ourToken) must returnValueLike[Either[RegistrationError, List[Versions.Version]]]{
        case Left(VersionsRetrievalFailed ) => ok
      }
    }
    "getting their version details" >> new TestScope {
      client.getTheirVersionDetails(uri, ourToken) must returnValueLike[Either[RegistrationError, Versions.VersionDetails]]{
        case Left(VersionDetailsRetrievalFailed ) => ok
      }
    }
    "sending credentials" >> new TestScope {
      client.sendCredentials(Url("url"), ourToken, creds) must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
        case Left(SendingCredentialsFailed ) => ok
      }
    }
    "updating credentials" >> new TestScope {
      client.updateCredentials(Url("url"), ourToken, creds) must returnValueLike[Either[RegistrationError, Creds[Theirs]]]{
        case Left(UpdatingCredentialsFailed ) => ok
      }
    }
  }

  trait TestScope extends Scope {
    implicit val httpExt = mock[HttpExt]
    httpExt.singleRequest(any(), any(), any(), any()) returns Future.failed(new RuntimeException)

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

    val client = new RegistrationClient[IO]()
  }
}

