package ch.epfl.core

import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.Utils.loadApplicationsLibrary
import ch.epfl.core.utils.{Constants, FileUtils}
import ch.epfl.core.verifier.exceptions.{VerifierError, VerifierInfo, VerifierMessage, VerifierWarning}

import java.nio.file.Path
import scala.annotation.tailrec
import mainargs.{main, arg, ParserForClass, Flag, TokensReader}

object Main {

  sealed trait Task
  case object Compile extends Task
  case object GenerateBindings extends Task

  implicit object TaskRead
      extends TokensReader[Task](
        "command",
        strs =>
          strs.head match {
            case "compile"          => Right(Compile)
            case "generateBindings" => Right(GenerateBindings)
            case token: String      => Left(token)
          }
      )

  @main
  case class Config(
      @arg(name = "task", short = 't', doc = "The task to run", positional = true)
      task: Task,
      @arg(name = "etsProjectFile", short = 'f', doc = "The ETS project file to use", positional = true)
      etsProjectFile: String
  )

  /** args = [compile | generateBindings] ets_proj_file.knxproj
    */
  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args)

    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH_STRING)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH_STRING)

    val newPhysicalStructure = EtsParser.parseEtsProjectFile(config.etsProjectFile)
    val existingPhysStructPath = Path.of(existingAppsLibrary.path).resolve(Path.of(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME))
    val existingPhysicalStructure = if (existingPhysStructPath.toFile.exists()) PhysicalStructureJsonParser.parse(existingPhysStructPath.toString) else PhysicalStructure(Nil)

    config.task match {
      case Compile =>
        val (compiledNewApps, compiledExistingApps, gaAssignment) = compiler.Compiler.compile(newAppsLibrary, existingAppsLibrary, newPhysicalStructure)
        val verifierMessages = verifier.Verifier.verify(compiledNewApps, compiledExistingApps, gaAssignment)
        if (validateProgram(verifierMessages)) {
          // Copy new app + all files in app_library
          FileUtils.moveAllFileToOtherDirectory(Constants.GENERATED_FOLDER_PATH_STRING, existingAppsLibrary.path)
          printTrace(verifierMessages)
        } else {
          // New App is rejected
          printTrace(verifierMessages)
        }
      case GenerateBindings =>
        compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
    }
  }

  @tailrec
  def validateProgram(messages: List[VerifierMessage]): Boolean = {
    messages match {
      case head :: tl =>
        head match {
          case _: VerifierError   => false
          case _: VerifierWarning => validateProgram(tl)
          case _: VerifierInfo    => validateProgram(tl)
        }
      case Nil => true
    }
  }

  @tailrec
  def printTrace(messages: List[VerifierMessage]): Unit = {
    messages match {
      case head :: tl => {
        head match {
          case error: VerifierError     => println(s"ERROR: ${error.msg}")
          case warning: VerifierWarning => println(s"WARNING: ${warning.msg}")
          case info: VerifierInfo       => println(s"INFO: ${info.msg}")
          case _                        => ()
        }
        printTrace(tl)
      }
      case Nil => ()
    }
  }
}
