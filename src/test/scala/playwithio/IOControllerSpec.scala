package playwithio

import scala.concurrent.{Await, Future}

import org.scalatestplus.play._

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import scala.concurrent.duration._

class MyControllerSpec extends PlaySpec with Results with Fixtures {

  "MyController#get" should {
    "be valid" in {
      val controller = new MyController(fastService)(stubControllerComponents())
      val result: Future[Result] = controller.get("hello").apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText mustBe "hello"
    }
  }

  "MyController#get" should {
    "fail" in {
      val controller = new MyController(slowService)(stubControllerComponents())
      val result: Future[Result] = controller.get("hello").apply(FakeRequest())

      val bodyText: String = contentAsString(result)

      bodyText mustBe "timeout"
    }
  }

  "MyController#get" should {
    "interrupt execution flow" in {
      val service = new Service(3.seconds)
      val controller = new MyController(service)(stubControllerComponents())
      val result: Future[Result] = controller.get("hello").apply(FakeRequest())

      Await.ready(result, 2.seconds)
      service.called mustBe false
    }
  }

  "MyController#get" should {
    "continue execution flow" in {
      val service = new Service(1.millis)
      val controller = new MyController(service)(stubControllerComponents())
      val result: Future[Result] = controller.get("hello").apply(FakeRequest())

      Await.ready(result, 2.seconds)
      service.called mustBe true
    }
  }

}

trait Fixtures {

  import scalaz.effect.IO

  val slowService = new UserService {
    def fetchUser(id: String): IO[String] = IO.sync(id).delay(3.seconds)    
  }

  val fastService = new UserService {
    def fetchUser(id: String): IO[String] = IO.sync(id)
  }

  class Service(delay: Duration) extends UserService{
    var called = false 
    def fetchUser(id: String): IO[String] = IO.sync(id).delay(delay) 
    override def doSomething(): IO[Unit] = {
      IO.sync{
        called = true
      }
    }
  }
}