package ch.epfl.web.service.main.utils

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

  implicit object TaskRead
      extends TokensReader[Task](
        "command",
        strs =>
          strs.head.toLowerCase match {
            case "run" | "r"   => Right(Run)
            case token: String => Left(token)
          }
      )

  @main(name = "svshi-webservice", doc = "Verification applications dedicated web service")
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
        doc = "The task to run. Can be passed as is. Possible options are 'run'. This argument is not case sensitive.",
        positional = true
      )
      task: Task,
      @arg(name = "address", short = 'a', doc = "The address and port for this particular server. The correct format is 'address:port' (ex: 192.168.1.1:5555)")
      addressPort: Option[String] = None,
      @arg(doc = "flag indicating that the webservice will run in the docker compose environment.")
      docker: Flag
  )
}
