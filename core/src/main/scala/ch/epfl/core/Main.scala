package ch.epfl.core

import ch.epfl.core.models.physical.PhysicalStructure
import ch.epfl.core.parsers.ets.EtsParser
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.{Constants, FileUtils}
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.verifier.bindings.exceptions.BindingsVerifierMessage
import ch.epfl.core.verifier.exceptions.VerifierMessage
import ch.epfl.core.verifier.exceptions.VerifierError
import ch.epfl.core.verifier.exceptions.VerifierWarning
import ch.epfl.core.verifier.static.python.exceptions.PythonVerifierMessage

import java.nio.file.Path
import scala.annotation.tailrec

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
    
    val newPhysicalStructure = EtsParser.parseEtsProjectFile(args(1))
    val existingPhysStructPath = Path.of(existingAppsLibrary.path).resolve(Path.of(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME))
    val existingPhysicalStructure = if(existingPhysStructPath.toFile.exists()) PhysicalStructureJsonParser.parse(existingPhysStructPath.toString) else PhysicalStructure(Nil)
    if (task == "compile") {
      val (compiledNewApps, compiledExistingApps, gaAssignment) = compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, newPhysicalStructure)
      val verifierMessages = verifier.Verifier.verify(compiledNewApps, compiledExistingApps, gaAssignment)
      if(validateProgram(verifierMessages)){
        // Copy new app + all files in app_library
        FileUtils.moveAllFileToOtherDirectory(Constants.GENERATED_FOLDER_PATH_STRING, existingAppsLibrary.path)
        printTrace(verifierMessages)
      } else {
        // new App is rejected
        printTrace(verifierMessages)
      }
    } else if (task == "generatebindings") {
      compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
    } else {
      println(s"Unknown task $task! Exiting...")
    }

  }

  @tailrec
  def validateProgram(messages: List[VerifierMessage]): Boolean = {
    messages match {
      case head :: tl => head match {
        case _:VerifierError => false
        case _:VerifierWarning => validateProgram(tl)
      }
      case Nil => true
    }
  }
  @tailrec
  def printTrace(messages: List[VerifierMessage]): Unit = {
    messages match {
      case head :: tl => {
        head match {
          case error: VerifierError => println(s"ERROR: ${error.msg}")
          case warning: VerifierWarning => println(s"WARNING: ${warning.msg}")
          case _ => ()
        }
        printTrace(tl)
      }
      case Nil => ()
    }
  }

  main()
}
