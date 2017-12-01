package playwithio

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.mvc._

import scalaz.effect.IO

trait UserService {
  def fetchUser(id: String): IO[String]

  def doSomething(): IO[Unit] = IO.unit
}

class MyController(userService: UserService)(controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) {

  def get(id: String) = TimeoutedAction().asyncIO(1.second) { req =>
    for {
      user <- userService.fetchUser(id)
      _ <- userService.doSomething
    } yield Ok(user)
  }

}
