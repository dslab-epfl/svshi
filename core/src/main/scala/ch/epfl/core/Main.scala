package ch.epfl.core

import ch.epfl.core.compiler.parsers.ets.EtsParser
import ch.epfl.core.models.application.ApplicationLibrary

object Main extends App {
  def loadLibrary(): ApplicationLibrary = {
    //TODO

    ApplicationLibrary(Nil)
  }





  def main(): Unit = {
    val library = loadLibrary()
    // parse PhysicalStructure here
    val physicalStructure = EtsParser.parseEtsProjectFile("ets_file.knxproj")

    // one time generate, one time compile !!!!!

    compiler.Compiler.generateBindingsFiles(library, physicalStructure)
    val compiledLibrary = compiler.Compiler.compile(library, physicalStructure)

    val verifiedLibrary = verifier.Verifier.verify(compiledLibrary)
    // DONE
  }

  main()
}
