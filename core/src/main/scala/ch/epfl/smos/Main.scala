package ch.epfl.smos

import ch.epfl.smos.KNXProgramming.Programming

object Main extends App {
  println(greeting)

  def greeting: String = "hello, SMOS!"

  Programming.prettifyConfig()
}
