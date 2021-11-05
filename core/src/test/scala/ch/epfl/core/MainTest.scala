package ch.epfl.core

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MainTest extends AnyFlatSpec with Matchers {
  "The Main object" should "say hello, SMOS!" in {
    Main.greeting shouldEqual "hello, SMOS!"
  }
}