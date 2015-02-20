import akka.http.model._
import akka.http.model.StatusCodes._
import akka.http.server.Directives._
import akka.http.server._
import akka.http.server.directives.AuthenticationDirectives._
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import com.optrak.authentication.Authentication.User
import com.optrak.authentication.{RequestBodies, AuthenticationActor, AuthenticationSupport, SecurityModel}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait TestAuth extends AuthenticationSupport {
  implicit val execCtx: ExecutionContext
  implicit val materializer: FlowMaterializer

  val authRoute =
    authenticateOrRejectWithChallenge(myUserPassAuthenticator).apply { user: User =>
      (pathPrefix("test") & put & parameter('msg.as[String])) { msg =>
        complete(OK -> s"user ${user.name} sent a message $msg")
      }
    }

}
