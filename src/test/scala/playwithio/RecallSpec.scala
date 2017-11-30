package playwithio

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scalaz.effect.{IO, RTS}

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalatest._

class RecallSpec extends FlatSpec with Matchers with RTS {

  class DB[A] {
    private var _items = Vector.empty[A]
    def add(item: A): Unit = synchronized {
      _items = _items :+ item
    }
    def find(predicate: A => Boolean): Seq[A] = synchronized {
      _items.filter(predicate)
    }
  }

  class DBFuture[A](db: DB[A]) {
    def add(item: A): Future[Unit] = Future {
      db.add(item)
    }
    def find(predicate: A => Boolean): Future[Seq[A]] = Future {
      db.find(predicate)
    }
  }

  def await[A](fut: Future[A]): A = Await.result(fut, Duration.Inf)

  "Recalling a Future" should "do the action once" in {
    val db = new DB[Int]
    val dbFuture = new DBFuture(db)
    val res: Future[Seq[Int]] =
      for {
        _ <- dbFuture.add(1)
        _ <- dbFuture.add(2)
        els <- dbFuture.find(_ % 2 == 0)
      } yield els
    await(res) shouldEqual Vector(2)
    await(res) shouldEqual Vector(2)
  }

  class DBIO[A](db: DB[A]) {
    def add(item: A): IO[Unit] = IO.sync {
      db.add(item)
    }
    def find(predicate: A => Boolean): IO[Seq[A]] = IO.sync {
      db.find(predicate)
    }
  }

  "Recalling a IO" should "do the action twice" in {
    val db = new DB[Int]
    val dbIO = new DBIO(db)
    val res: IO[Seq[Int]] =
      for {
        _ <- dbIO.add(1)
        _ <- dbIO.add(2)
        els <- dbIO.find(_ % 2 == 0)
      } yield els
    unsafePerformIO(res) shouldEqual Vector(2)
    unsafePerformIO(res) shouldEqual Vector(2, 2)
  }

}
