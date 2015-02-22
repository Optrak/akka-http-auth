import java.io.IOException

import akka.actor.{ActorRef, ActorSystem}
import akka.agent.Agent
import akka.http.Http
import akka.http.model.StatusCodes._
import akka.http.model.headers.BasicHttpCredentials
import akka.http.server.Directives._
import akka.http.server.directives.AuthenticationDirectives._
import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import com.optrak.authentication.Authentication.User
import com.optrak.authentication.AuthenticationActor._
import com.optrak.authentication._

import scala.concurrent.{Await, ExecutionContext, Future}

trait TestAuth extends AuthenticationSupport {
  implicit def execCtx: ExecutionContext
  implicit def materializer: FlowMaterializer

  val authRoute =
    authenticateOrRejectWithChallenge(myUserPassAuthenticator).apply { user: User =>
      (pathPrefix("test") & (put | get) & parameter('msg.as[String])) { msg =>
        complete(OK -> s"user ${user.name} sent a message $msg")
      }
    }

}

object TestApp extends TestAuth {
  implicit val system = ActorSystem()
  implicit val materializer = ActorFlowMaterializer() //implicit here is IMPORTANT
  implicit val execCtx = system.dispatcher
  val authenticationHolder = new AuthenticationHolder(system)
  val securityAgent: Agent[SecurityModel] = authenticationHolder.securityAgent
  val authStrategy = new AuthStrategy(securityAgent)
  def auth: ActorRef = authenticationHolder.auth // AuthenticatorActor
  auth ! Bootstrap

  def main(args: Array[String]): Unit = {
    /*
    initialUserName = "bootstrap"
    initialPassword = "secret&h1dden"
     */
    val credentials = BasicHttpCredentials(defaultUserName, defaultPassword)

    val binding = Http().bind(interface = "localhost", port = 8080)


    val materializedMap = binding startHandlingWith {
      authRoute
    }
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

    Console.readLine()
    binding.unbind(materializedMap).onComplete(_ ⇒ system.shutdown())
  }
}