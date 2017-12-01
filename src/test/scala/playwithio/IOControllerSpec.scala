package playwithio

import scala.concurrent.Future

import org.scalatestplus.play._

import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._

class MyControllerSpec extends PlaySpec with Results {

  "MyController#get" should {
    "should be valid" in {
      val controller = new MyController(stubControllerComponents())
      val result: Future[Result] = controller.get("hello").apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      bodyText mustBe "hello"
    }
  }
}