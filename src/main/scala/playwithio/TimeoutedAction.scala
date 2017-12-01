package playwithio

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration
import scalaz.effect.{IO, RTS}
import scalaz.effect.Errors.TimeoutException
import play.api.mvc._

case class TimeoutedAction[B](
  bodyParser: BodyParser[B] = BodyParsers.utils.ignore[AnyContent](AnyContentAsEmpty)
)
(implicit ec: ExecutionContext) extends ActionBuilderImpl[B](bodyParser) with RTS {
  
  def asyncIO(duration: Duration)(block: Request[B] => IO[Result]): Action[B] = {
    async(parser){ request =>       
      val timeouted = block(request)
        .timeout(duration).catchSome {
          case t: TimeoutException =>
            IO.point(Results.RequestTimeout)
        }
      
      Future { unsafePerformIO( timeouted ) }
    }
  }

} 
