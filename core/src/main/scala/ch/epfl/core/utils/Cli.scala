package ch.epfl.core.utils

import mainargs.{main, arg, TokensReader, Flag}

/** Provide CLI helpers
  */
object Cli {

  /** A task that can be run with the CLI
    */
  sealed trait Task

  /** Task that runs all the apps and the runtime verification
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
            case token: String                                  => Left(token)
          }
      )

  @main(name = "svshi", doc = "A platform for developing and running formally verified smart infrastructures")
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
        short = 't',
        doc = "The task to run. Can be passed without the flag. Possible options are 'run', 'compile', 'generateBindings', 'generateApp' and 'listApps'",
        positional = true
      )
      task: Task,
      @arg(name = "ets-file", short = 'f', doc = "The ETS project file to use for the tasks 'compile' and 'generateBindings'")
      etsProjectFile: Option[String] = None,
      @arg(name = "app-name", short = 'n', doc = "The app name to use for the task 'generateApp'")
      appName: Option[String] = None,
      @arg(name = "no-colors", doc = "The flag to disable output coloring")
      noColors: Flag
  )
}
