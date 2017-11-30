package playwithio

import scalaz.effect.RTS

import org.scalatest._

class HelloSpec extends FlatSpec with Matchers with RTS {

  "The Hello object" should "say hello" in {
    unsafePerformIO { Hello.getUrl } shouldEqual "hello"
  }

}
