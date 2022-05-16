package ch.epfl.core.utils

import ch.epfl.core.utils.subprocess.{SvshiSubProcess, SvshiSubProcessOs}

import scala.util.{Failure, Success, Try}

/** Object containing functions to call external processes
  */
object ProcRunner {

  /** Execute the given python module as a new process and return the exit code along with the stdout and stderr lines combined.
    * It can optionally write stdOut and/or stdErr to some files. If defined, it redirects the corresponding output
    * to the passed file
    * @param stdOut: Optionally, a function to execute on each new line of stdOut
    * @param stdErr: Optionally, a function to execute on each new line of stdErr
    * @param pythonModule: The python module to call
    * @param wd: The working directory from which to call the python module
    * @param args: The args to pass to the python module
    * @return
    */
  def callPythonBlocking(stdOut: Option[String => Unit], stdErr: Option[String => Unit], pythonModule: String, wd: os.Path, args: String*): (Int, List[String]) = {
    val invoked = (stdOut, stdErr) match {
      case (None, None)                 => Try(os.proc("python3", "-m", pythonModule, args).call(cwd = wd))
      case (Some(stdOutCallback), None) => Try(os.proc("python3", "-m", pythonModule, args).call(cwd = wd, stdout = os.ProcessOutput.Readlines(stdOutCallback)))
      case (None, Some(stdErrCallback)) => Try(os.proc("python3", "-m", pythonModule, args).call(cwd = wd, stderr = os.ProcessOutput.Readlines(stdErrCallback)))
      case (Some(stdOutCallback), Some(stdErrCallback)) =>
        Try(os.proc("python3", "-m", pythonModule, args).call(cwd = wd, stdout = os.ProcessOutput.Readlines(stdOutCallback), stderr = os.ProcessOutput.Readlines(stdErrCallback)))
    }

    invoked match {
      case Failure(exception: os.SubprocessException) =>
        val result = exception.result
        (result.exitCode, result.out.lines.toList.map(e => f"stdout: $e") ++ result.err.lines.toList.map(e => f"stderr: $e"))
      case Success(result)    => (result.exitCode, result.out.lines.toList)
      case Failure(exception) => throw exception
    }
  }

  /** Execute the given python module as a new process and return the subprocess instance for further usage. This call
    * is therefore non blocking.
    * It can optionally write stdOut and/or stdErr to some files. If defined, it redirects the corresponding output
    * to the passed file
    * @param stdOut: Optionally, a function to execute on each new line of stdOut
    * @param stdErr: Optionally, a function to execute on each new line of stdErr
    * @param pythonModule: The python module to call
    * @param wd: The working directory from which to call the python module
    * @param args: The args to pass to the python module
    * @return
    */
  def callPythonNonBlocking(stdOut: Option[String => Unit], stdErr: Option[String => Unit], pythonModule: String, wd: os.Path, args: String*): SvshiSubProcess = {
    val osSubProcess = (stdOut, stdErr) match {
      case (None, None)                 => os.proc("python3", "-m", pythonModule, args).spawn(cwd = wd)
      case (Some(stdOutCallback), None) => os.proc("python3", "-m", pythonModule, args).spawn(cwd = wd, stdout = os.ProcessOutput.Readlines(stdOutCallback))
      case (None, Some(stdErrCallback)) => os.proc("python3", "-m", pythonModule, args).spawn(cwd = wd, stderr = os.ProcessOutput.Readlines(stdErrCallback))
      case (Some(stdOutCallback), Some(stdErrCallback)) =>
        os.proc("python3", "-m", pythonModule, args).spawn(cwd = wd, stdout = os.ProcessOutput.Readlines(stdOutCallback), stderr = os.ProcessOutput.Readlines(stdErrCallback))
    }
    new SvshiSubProcessOs(osSubProcess)

  }

  /** Execute CrossHair check on the given python (i.e., .py) file from the given working directory and with the given timeout in seconds.
    * Returns the exit code, the stdout's AND stderr's lines concatenated (first stdout's then stderr's)
    * @param filePath
    * @param wd
    * @param perConditionTimeoutSeconds
    * @return
    */
  def callCrosshair(filePath: String, wd: os.Path, perConditionTimeoutSeconds: Int): (Int, List[String]) = {
    val invoked = os.proc("python3", "-m", "crosshair", "check", filePath, "--report_all", "--per_condition_timeout", s"$perConditionTimeoutSeconds").call(cwd = wd, check = false)
    (invoked.exitCode, invoked.out.lines.toList ++ invoked.err.lines.toList)
  }
}
