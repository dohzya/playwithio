package playwithio

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import play.api.mvc._

class MyController(controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) {

  def get(id: String) = TimeoutedAction().asyncIO(1.second) { req =>
    for {
      user <- Hello.fetchUser(id)
    } yield Ok(user)
  }

}
