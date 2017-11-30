package playwithio

import scala.concurrent.Future
import scalaz.effect.{Errors, IO, RTS, SafeApp}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scalaz.data.Disjunction
import scalaz.data.Disjunction.{-\/, \/-}
import scala.concurrent.duration._
import scala.util.Random
import scalaz.effect.Errors.TimeoutException

import lol.http.Client
import lol.http.Get
import play.api.mvc._

object Hello extends Greeting with SafeApp {

  IO.now(pureFn) // strict (or eager)
  IO.point(pureFn) // lazy
  IO.sync(impureFn())
  IO.async[Int] { cb =>
    new Thread(() => {
      Thread.sleep(500)
      cb(\/-(3))
    }).start()
  } // equivalent to next
  IO.sleep(500.millis) // doesn't use a thread (ScheduledExecutorService.schedule)

  IO.sleep(500.millis).flatMap(_ => ioFn()) // equivalent to next
  ioFn().delay(500.millis)

  for { // sequential calls
    a <- getValueFromWS("http://google.com")
    b <- fetchInDB(a)
  } yield b

  for { // parallel calls (NOT IN CATS)
    fiber <- getValueFromWS("http://google.com").fork
    b <- getValueFromWS("http://facebook.com")
    a <- fiber.join
  } yield (a, b)

  ioFn().race(ioFn()) // return the fastest and kill the slowest (NOT IN CATS)

  ioFn().timeout(500.millis) // kill the IO and return a failed IO (NOT IN CATS)

  def doSomethingInMyBack(): Unit = {
    // the println will be called
    Future {
      println("don't print me!")
    }
    ()
  }
  def doNothingInMyBack(): Unit = {
    // the println won't be called
    IO.sync {
      println("don't print me!")
    }
    ()
  }

  openFile("data.json").bracket(closeFile) { file =>
    for {
      header <- readHeader(file)
      // ...
    } yield header
  }

  trait MyController extends BaseController with RTS {

    def get(id: String) = TimeoutedAction(1.second) { req =>
      for {
        user <- fetchUser(id)
      } yield Ok(user)
    }

    def TimeoutedAction[A](dur: Duration)(fn: Request[A] => IO[Result]) =
      Action.async { req =>
        val timeouted = fn(req).timeout(1.second).catchSome {
          case t: TimeoutException =>
            IO.point(Results.RequestTimeout)
        }
        // Can't create a future from a scalaz's IO, so this code will be blocking and consume a thread
        Future { unsafePerformIO(timeouted) }
        // With cats's IO, we would call io.unsafeToFuture()
      }

  }



  // ---------------------------------------------------------------------------

  def pureFn: Int = 3
  def impureFn(): Int = Random.nextInt()
  def ioFn(): IO[Int] = IO.sync(impureFn())

  def getValueFromWS(url: String): IO[String] = IO.sync("")
  def fetchInDB(content: String): IO[String] = IO.sync("")
  case class File(path: String)
  def openFile(path: String): IO[File] = IO.point(File(path))
  def closeFile(file: File): IO[Unit] = IO.unit
  def readHeader(file: File): IO[String] = IO.sync("")
  def fetchUser(id: String): IO[String] = IO.sync("")



  case object Timeouted extends Exception("TIMEOUTED")

  def sleep(dur: Duration, msg: Any) =
    for {
      //_ <- IO.sleep(dur).onError {
      _ <- IO.async[Unit] { cb =>
        new Thread(() => {
          Thread.sleep(dur.toMillis)
          println("dans le sleep")
          cb(\/-(()))
        }).start()
      }.onError {
        case Timeouted => IO.sync { println("OH NOW I HAVE BINE KILAIDE") }
        case other => IO.sync { println(s"Error: $other") }
      }
      //_ <- IO.sleep(dur).bracket(_ => IO.sync { println("done") })(_ => IO.unit)
      _ <- IO.sync { println(msg) }
    } yield ()

  //override def run(args: List[String]): IO[Unit] =
  //  IO.supervise(
  //    for {
  //      _ <- IO.sync { println(1) }
  //      fiber1 <- sleep(3.second, 2).fork
  //      _ <- sleep(1.second, 3)
  //      //_ <- fiber1.join
  //    } yield ()
  //    , KILAIDE
  //  )

  //override def run(args: List[String]): IO[Unit] =
  //  for {
  //    _ <- IO.sync { println(1) }
  //    _ <- {
  //      val io1 = IO.sleep(1.second)
  //      val io2 = sleep(3.second, 3)
  //      io1.race(io2)
  //      io1.raceWith(io2) {
  //        case -\/((_, fiber)) => fiber.interrupt(KILAIDE)
  //        case \/-((_, fiber)) => fiber.interrupt(KILAIDE)
  //      }
  //    }
  //  } yield ()

  def timeout[A](io: IO[A], dur: Duration, error: Throwable): IO[A] = {
    io.raceWith(IO.sleep(dur)) {
      case -\/((a, fiber)) => fiber.interrupt(new Exception()).const(a)
      case \/-(((), fiber)) => fiber.interrupt(error).flatMap { _ => IO.fail(error) }
    }
  }

  override def run(args: List[String]): IO[Unit] =
    for {
      _ <- IO.sync { println(1) }
      _ <- sleep(3.second, 3).timeout(1.second).catchAll(_ => IO.unit)
      _ <- timeout(sleep(3.second, 3), 1.second, Timeouted).catchAll(_ => IO.unit)
    } yield ()


  def wrapCatsIO[A](catsIO: cats.effect.IO[A]): IO[A] = {
    IO.async { cb =>
      catsIO.unsafeRunAsync { either =>
        val disjunction = Disjunction.fromEither(either)
        cb(disjunction)
      }
    }
  }

  //override def run(args: List[String]): IO[Unit] = for {
  //  url <- getUrl
  //  _ <- IO.sync { println(1) }
  //  //mvar <- IO.emptyMVar
  //  fiber <- httpGet(url).fork
  //  _ <- IO.sync { println(2) }
  //  body <- fiber.join
  //  _ <- IO.sync { println(body) }
  //  _ <- IO.sync { println(3) }
  //} yield ()

  def httpGet(url: String): IO[String] = {
    wrapCatsIO(Client.run(Get(url)) { res =>
      res.readAs[String].map { contentBody =>
        println("httpGet")
        contentBody.take(10)
      }
    })
  }

}

trait Greeting {

  //lazy val getUrl: IO[String] = IO.point("http://dohzya.com")
  lazy val getUrl: IO[String] = IO.point("hello")

}
