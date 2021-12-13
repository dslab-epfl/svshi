package ch.epfl.core.verifier.static.python

import scala.util.Try
import scala.util.Failure
import scala.util.Success

/** Object containing functions to call external processes
  */
object ProcRunner {

  /** Execute the given python module as a new process and return the exit code along with the stdout lines
    * @param pythonModule
    * @return
    */
  def callPython(pythonModule: String, wd: os.Pathargs: String*): (Int, List[String]) = {
    val invoked = Try(os.proc("python3", "-m", pythonModule, args).call(cwd = wd))
    invoked match {
      case Failure(exception: os.SubprocessException) =>
        val result = exception.result
        (result.exitCode, result.out.lines.toList)
      case Success(result)    => (result.exitCode, result.out.lines.toList)
      case Failure(exception) => throw exception
    }
  }

  /** Execute CrossHair check on the given python (i.e., .py) file from the given working directory and with the given timeout in seconds.
    * Returns the exit code, the stdout's AND stderr's lines concatenated (first stdout's then stderr's)
    * @param filePath
    * @param wd
    * @param perConditionTimeoutSeconds
    * @return
    */
  def callCrosshair(filePath: String, wd: os.Path, perConditionTimeoutSeconds: Int): (Int, List[String]) = {
    val invoked = os.proc("crosshair", "check", filePath, "--report_all", "--per_condition_timeout", s"$perConditionTimeoutSeconds").call(cwd = wd, check = false)
    (invoked.exitCode, invoked.out.lines.toList ++ invoked.err.lines.toList)
  }
}
