package playwithio

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scalaz.effect.{IO, RTS}
import scalaz.effect.Errors.TimeoutException
import play.api.mvc._

class TimeoutedAction[B](
  bodyParser: BodyParser[B]
)(implicit ec: ExecutionContext) extends ActionBuilderImpl[B](bodyParser) with RTS {
  
  def asyncIO(duration: Duration)(block: Request[B] => IO[Result]): Action[B] = {
    async(parser){ request =>       
      val timeouted = block(request)
        .timeout(duration).catchSome {
          case t: TimeoutException =>
            IO.point(Results.RequestTimeout("timeout"))
        }
      
      Future { unsafePerformIO( timeouted ) }
    }
  }

} 

object TimeoutedAction {

  def apply()(implicit ec: ExecutionContext) = new TimeoutedAction( BodyParsers.utils.ignore[AnyContent](AnyContentAsEmpty) )

  def apply[A](bodyParser: BodyParser[A])(implicit ec: ExecutionContext) = new TimeoutedAction[A](bodyParser)
}