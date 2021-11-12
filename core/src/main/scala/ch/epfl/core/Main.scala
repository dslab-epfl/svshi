package ch.epfl.core

import ch.epfl.core.compiler.parsers.ets.EtsParser
import ch.epfl.core.models.ApplicationLibrary

object Main extends App {
  val libraryPath : String = "../library"
  def loadLibrary(path: String): ApplicationLibrary = {
    //TODO
    ApplicationLibrary(Nil)
  }





  def main(): Unit = {
    val library = loadLibrary(libraryPath)
    val compiledLibrary = verifier.Verifier.verify(compiler.Compiler.compile(library))
    // DONE
  }


}
