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
import ch.epfl.core.verifier.static.python.ProcRunner

object Main {

  sealed trait Task
  case object Run extends Task
  case object Compile extends Task
  case object GenerateBindings extends Task
  case object GenerateApp extends Task
  case object ListApps extends Task

  implicit object TaskRead
      extends TokensReader[Task](
        "command",
        strs =>
          strs.head match {
            case "run"                                   => Right(Run)
            case "compile"                               => Right(Compile)
            case "generateBindings" | "generatebindings" => Right(GenerateBindings)
            case "generateApp" | "generateapp"           => Right(GenerateApp)
            case "listApps" | "listapps"                 => Right(ListApps)
            case token: String                           => Left(token)
          }
      )

  @main
  case class Config(
      @arg(name = "task", short = 't', doc = "The task to run", positional = true)
      task: Task,
      @arg(name = "etsProjectFile", short = 'f', doc = "The ETS project file to use for the tasks 'compile' and 'generateBindings'")
      etsProjectFile: Option[String] = None,
      @arg(name = "appName", short = 'n', doc = "The app name to use for the task 'generateApp'")
      appName: Option[String] = None
  )

  def main(args: Array[String]): Unit = {
    val config = ParserForClass[Config].constructOrExit(args)

    val existingAppsLibrary = loadApplicationsLibrary(APP_LIBRARY_FOLDER_PATH_STRING)
    val newAppsLibrary = loadApplicationsLibrary(GENERATED_FOLDER_PATH_STRING)

    val existingPhysStructPath = Path.of(existingAppsLibrary.path).resolve(Path.of(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME))
    val existingPhysicalStructure = if (existingPhysStructPath.toFile.exists()) PhysicalStructureJsonParser.parse(existingPhysStructPath.toString) else PhysicalStructure(Nil)

    config.task match {
      case Run => runPythonModule(RUNTIME_PYTHON_MODULE, Seq(), exitCode => s"The runtime module failed with exit code $exitCode and above stdout")
      case Compile | GenerateBindings if config.etsProjectFile.isEmpty =>
        printErrorAndExit("The ETS project file needs to be specified for compiling or generating the bindings")
      case Compile =>
        val newPhysicalStructure = EtsParser.parseEtsProjectFile(config.etsProjectFile.get)
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
        val newPhysicalStructure = EtsParser.parseEtsProjectFile(config.etsProjectFile.get)
        compiler.Compiler.generateBindingsFiles(newAppsLibrary, existingAppsLibrary, newPhysicalStructure, existingPhysicalStructure)
      case GenerateApp =>
        config.appName match {
          case Some(name) =>
            val nameRegex = "^_*[a-z]+[a-z_]*_*$".r
            if (nameRegex.matches(name)) runPythonModule(APP_GENERATOR_PYTHON_MODULE, Seq(name), exitCode => s"The app generator failed with exit code $exitCode and above stdout")
            else printErrorAndExit("The app name has to contain only lowercase letters and underscores")
          case None =>
            printErrorAndExit("The app name has to be provided for generating a new app")
        }
      case ListApps =>
        val appNames = existingAppsLibrary.apps.map(_.name)
        if (appNames.isEmpty) println("There are no apps installed!")
        else println(s"The installed apps are: ${appNames.mkString(",")}")
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
          case error: VerifierError     => printErrorAndExit(error.msg)
          case warning: VerifierWarning => println(fansi.Color.Yellow(s"WARNING: ${warning.msg}"))
          case info: VerifierInfo       => println(fansi.Color.LightBlue(s"INFO: ${info.msg}"))
          case _                        => ()
        }
        printTrace(tl)
      }
      case Nil => ()
    }
  }

  private def printErrorAndExit(errorMessage: String): Unit = {
    println(fansi.Color.Red(s"ERROR: $errorMessage"))
    sys.exit(1)
  }

  private def runPythonModule(module: String, args: Seq[String], errorMessageBuilder: Int => String): Unit = {
    val (exitCode, stdOut) = ProcRunner.callPython(module, args: _*)
    if (exitCode != 0) printErrorAndExit(errorMessageBuilder(exitCode))
  }
}
