package ch.epfl.core.utils

import mainargs.{Flag, TokensReader, arg, main}

/** Provide CLI helpers
  */
object Cli {

  /** A task that can be run with the CLI
    */
  sealed trait Task

  /** Task that runs all the apps with runtime verification
    */
  case object Run extends Task

  /** Task that compiles all the apps
    */
  case object Compile extends Task

  /** Task that generates the bindings for all the apps
    */
  case object GenerateBindings extends Task

  /** Task that generates a Python app skeleton
    */
  case object GenerateApp extends Task

  /** Task that lists all the installed apps
    */
  case object ListApps extends Task

  /** Task that outputs the current CLI version
    */
  case object GetVersion extends Task

  implicit object TaskRead
      extends TokensReader[Task](
        "command",
        strs =>
          strs.head match {
            case "run" | "r"                                    => Right(Run)
            case "compile" | "c"                                => Right(Compile)
            case "generateBindings" | "generatebindings" | "gb" => Right(GenerateBindings)
            case "generateApp" | "generateapp" | "ga"           => Right(GenerateApp)
            case "listApps" | "listapps" | "la"                 => Right(ListApps)
            case "version" | "v"                                => Right(GetVersion)
            case token: String                                  => Left(token)
          }
      )

  @main(name = "svshi", doc = "Secure and Verified Smart Home Infrastructure")
  /** The CLI configuration
    *
    * @param task the task to run
    * @param etsProjectFile the ETS project file to use for the tasks 'compile' and 'generateBindings'
    * @param appName the app name to use for the task 'generateApp'
    * @param noColors the flag to disable output coloring
    */
  case class Config(
      @arg(
        name = "task",
        doc =
          "The task to run. Can be passed as is. Possible options are 'run', 'compile', 'generateBindings', 'generateApp', 'listApps' and 'version'. This argument is not case sensitive.",
        positional = true
      )
      task: Task,
      @arg(name = "ets-file", short = 'f', doc = "The ETS project file to use for the tasks 'compile' and 'generateBindings'")
      etsProjectFile: Option[String] = None,
      @arg(name = "devices-json", short = 'd', doc = "The devices prototypical structure JSON file to use for the task 'generateApp'")
      devicesPrototypicalStructureFile: Option[String] = None,
      @arg(name = "app-name", short = 'n', doc = "The app name to use for the task 'generateApp'")
      appName: Option[String] = None,
      @arg(name = "address", short = 'a', doc = "The KNX address to use for the task 'run'. The correct format is 'address:port' (ex: 192.168.1.1:5555)")
      knxAddress: Option[String] = None,
      @arg(name = "no-colors", doc = "The flag to disable output coloring")
      noColors: Flag
  )
}
