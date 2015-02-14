import akka.actor.{ActorSystem, ActorRef}
import akka.agent.Agent
import akka.http.Http
import akka.http.model.headers.BasicHttpCredentials
import akka.http.model.{StatusCodes, Uri}
import akka.http.server.Route
import com.optrak.authentication.Authentication._
import com.optrak.authentication.AuthenticationActor._
import com.optrak.authentication.{SecurityModel, AuthStrategy, AuthenticationHolder}
import com.optrak.authentication.RequestBodies.{AdminData, UserData}
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.Await
import scala.concurrent.duration._


class AuthenticationManagementSpec extends Specification with TestAuth with NoTimeConversions {
  sequential

  implicit val system = ActorSystem()

  val authenticationHolder = new AuthenticationHolder(system)
  Thread.sleep(200)
  val securityAgent: Agent[SecurityModel] = authenticationHolder.securityAgent
  val authStrategy = new AuthStrategy(securityAgent)
  def auth: ActorRef = authenticationHolder.auth // AuthenticatorActor

  def securityModel: SecurityModel = {
    val fut = securityAgent.future()
    Await.result(fut, timeout.duration)// .asInstanceOf[SecurityModel]
  }

  auth ! Bootstrap
  Thread.sleep(200)
  // TODO: sprinkle some Thread.sleep(200) ? <-- smelly!

  //val superUser: User = securityModel.findSysAdmin(defaultUserName, defaultPassword).err(s"must be there \n$securityModel")

  val secretPassword = "secre!TT1"
  val email = "hi@optrak.com"
  val credentials = BasicHttpCredentials(defaultUserName, defaultPassword)

  import scala.language.postfixOps

  Http().bind(interface = "localhost", port = 8080).startHandlingWith(Route.handlerFlow(authRoute))

}