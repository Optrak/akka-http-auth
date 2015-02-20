import java.io.IOException

import akka.agent.Agent
import akka.http.model.{HttpRequest, HttpResponse}
import akka.http.model.headers.BasicHttpCredentials
import akka.http.model.StatusCodes._
import akka.http.model.Uri._
import akka.http.client._
import RequestBuilding._
import akka.http.unmarshalling._
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.http.Http
import com.optrak.authentication.AuthenticationActor._
import com.optrak.authentication.{SecurityModel, AuthStrategy, AuthenticationHolder}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class TestAuthenticationSpec extends Specification with TestAuth with NoTimeConversions {
  implicit val system = ActorSystem()
  implicit val materializer = ActorFlowMaterializer() //implicit here is IMPORTANT
  implicit val execCtx = system.dispatcher

  val authenticationHolder = new AuthenticationHolder(system)

  val securityAgent: Agent[SecurityModel] = authenticationHolder.securityAgent
  val authStrategy = new AuthStrategy(securityAgent)
  def auth: ActorRef = authenticationHolder.auth // AuthenticatorActor
  auth ! Bootstrap

  val secretPassword = "secre!TT1"
  val email = "hi@optrak.com"
  val credentials = BasicHttpCredentials(defaultUserName, defaultPassword)

  val binding = Http().bind(interface = "localhost", port = 8080)



  val materializedMap = binding startHandlingWith {
    authRoute
  }
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")


  val localConnection = Http(system).outgoingConnection("localhost", 8080)

  def localRequest(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(localConnection.flow).runWith(Sink.head)

  def putTestMsg(msg: String, credentials: BasicHttpCredentials): Future[String] = {
    localRequest(Put("/test".withQuery("msg" -> msg)) ~> addCredentials(credentials)).flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String]
        case _ =>
          val error = s"Local authenticated request failed with status code ${response.status}"
          logger.error(error)
          Future.failed(new IOException(error))
      }
    }
  }


  "http server with auth route" should {
    "reply properly to authorized request" in {
      Await.result(putTestMsg("hello", credentials), 2 second) must_== s"user $defaultUserName sent a message hello"
    }

    "reply properly to unauthorized request" in {
      Await.result(putTestMsg("hello", BasicHttpCredentials("yar", "admin")), 2 second) must
        throwA[IOException]("Local authenticated request failed with status code 401 Unauthorized")
    }
  }

  //binding.unbind(materializedMap).onComplete(_ ⇒ system.shutdown())
}