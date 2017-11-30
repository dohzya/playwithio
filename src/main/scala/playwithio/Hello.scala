package playwithio

import scalaz.effect.{IO, SafeApp}

object Hello extends Greeting with SafeApp {

  override def run(args: List[String]): IO[Unit] = for {
    msg <- greeting
    _ <- IO.sync { println(msg) }
  } yield ()

}

trait Greeting {

  lazy val greeting: IO[String] = IO.point("hello")

}
