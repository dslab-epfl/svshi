package ch.epfl.core

import ch.epfl.core.compiler.parsers.ets.EtsParser
import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.utils.Utils.loadApplicationsLibrary

object Main extends App {
  /**
   * args = [compile | generateBindings] app_library_path ets_proj_file.knxproj
   */
  def main(): Unit = {
    if(args.length != 3) {
      println("Wrong number of arguments! Exiting...")
      return
    }
    val task = args(0).toLowerCase
    val library = loadApplicationsLibrary(args(1))
    // parse PhysicalStructure here
    val physicalStructure = EtsParser.parseEtsProjectFile(args(2))
    if(task == "compile"){
      val compiledLibrary = compiler.Compiler.compile(library, physicalStructure)
      val verifiedLibrary = verifier.Verifier.verify(compiledLibrary)
    } else if(task == "generateBindings"){
      compiler.Compiler.generateBindingsFiles(library, physicalStructure)
    } else {
      println(s"Unknown task $task! Exiting...")
    }

  }

  main()
}
