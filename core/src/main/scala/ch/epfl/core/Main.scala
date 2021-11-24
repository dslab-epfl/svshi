package ch.epfl.core

import ch.epfl.core.parsers.ets.EtsParser
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary

object Main extends App {

  /** args = [compile | generateBindings] ets_proj_file.knxproj
    */
  def main(): Unit = {
    if (args.length != 2) {
      println("Wrong number of arguments! Exiting...")
      return
    }
    val task = args(0).toLowerCase
    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH_STRING)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH_STRING)
    
    val physicalStructure = EtsParser.parseEtsProjectFile(args(1))
    if (task == "compile") {
      val (compiledNewApps, compiledExistingApps) = compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, physicalStructure)
      val verifiedLibrary = verifier.Verifier.verify(compiledNewApps, compiledExistingApps)
    } else if (task == "generatebindings") {
      compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, physicalStructure)
    } else {
      println(s"Unknown task $task! Exiting...")
    }

  }

  main()
}
