package ch.epfl.core

import ch.epfl.core.models.physical.PhysicalStructure
import ch.epfl.core.parsers.ets.EtsParser
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary

import java.nio.file.Path

object Main extends App {

  /** args = [compile | generateBindings] app_library_path ets_proj_file.knxproj
    */
  def main(): Unit = {
    if (args.length != 3) {
      println("Wrong number of arguments! Exiting...")
      return
    }
    val task = args(0).toLowerCase
    val existingAppsLibrary = loadApplicationsLibrary(args(1))
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH_STRING)
    
    val newPhysicalStructure = EtsParser.parseEtsProjectFile(args(2))
    val existingPhysStructPath = Path.of(existingAppsLibrary.path).resolve(Path.of(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME))
    println(existingPhysStructPath)
    val existingPhysicalStructure = if(existingPhysStructPath.toFile.exists()) PhysicalStructureJsonParser.parse(existingPhysStructPath.toString) else PhysicalStructure(Nil)
    if (task == "compile") {
      val (compiledNewApps, compiledExistingApps, gaAssignment) = compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, newPhysicalStructure)
      val verifiedLibrary = verifier.Verifier.verify(compiledNewApps, compiledExistingApps, gaAssignment)
    } else if (task == "generatebindings") {
      compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
    } else {
      println(s"Unknown task $task! Exiting...")
    }

  }

  main()
}
