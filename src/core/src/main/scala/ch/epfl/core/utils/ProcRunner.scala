package ch.epfl.core.utils

import ch.epfl.core.utils.subprocess.{SvshiSubProcess, SvshiSubProcessOs}

import scala.util.{Failure, Success, Try}

/** Object containing functions to call external processes
  */
object ProcRunner {

  /** Execute the given python module as a new process and return the exit code along with the stdout and stderr lines combined.
    * It can optionally write stdOutLines and/or stdErrLines to some files. If defined, it redirects the corresponding output
    * to the passed file
    * @param stdOutCallBack: Optionally, a function to execute on each new line of stdOutLines
    * @param stdErrCallBack: Optionally, a function to execute on each new line of stdErrLines
    * @param pythonModule: The python module to call
    * @param wd: The working directory from which to call the python module
    * @param args: The args to pass to the python module
    * @return
    */
  def callPythonBlocking(stdOutCallBack: Option[String => Unit], stdErrCallBack: Option[String => Unit], pythonModule: String, wd: os.Path, args: String*): (Int, List[String]) = {
    var stdOutLines: List[String] = Nil
    var stdErrLines: List[String] = Nil
    def fStdOut(s: String) = {
      stdOutLines = stdOutLines ++ List(s)
      stdOutCallBack.foreach(f => f(s))
    }
    def fStdErr(s: String) = {
      stdErrLines = stdErrLines ++ List(s)
      stdErrCallBack.foreach(f => f(s))
    }
    val invoked = Try(
      os.proc("python3", "-m", pythonModule, args).call(cwd = wd, stdout = os.ProcessOutput.Readlines(fStdOut), stderr = os.ProcessOutput.Readlines(fStdErr))
    )

    invoked match {
      case Failure(exception: os.SubprocessException) =>
        val result = exception.result
        (result.exitCode, stdOutLines.map(e => f"stdout: $e") ++ stdErrLines.map(e => f"stderr: $e"))
      case Success(result)    => (result.exitCode, stdOutLines.map(e => f"stdout: $e") ++ stdErrLines.map(e => f"stderr: $e"))
      case Failure(exception) => throw exception
    }
  }

  /** Execute the given python module as a new process and return the subprocess instance for further usage. This call
    * is therefore non blocking.
    * It can optionally write stdOutLines and/or stdErrLines to some files. If defined, it redirects the corresponding output
    * to the passed file
    * @param stdOutLines: Optionally, a function to execute on each new line of stdOutLines
    * @param stdErrLines: Optionally, a function to execute on each new line of stdErrLines
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
}
