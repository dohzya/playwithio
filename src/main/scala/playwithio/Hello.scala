package playwithio

import scalaz.effect.{IO, RTS, SafeApp}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scalaz.data.Disjunction
import scalaz.data.Disjunction.{-\/, \/-}
import scala.concurrent.duration._
import scala.util.Random

import lol.http.Client
import lol.http.Get

object Hello extends Greeting with SafeApp with RTS {

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
