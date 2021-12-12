package ch.epfl.core.verifier.static.python

/** Object containing functions to call external processes
  */
object ProcRunner {

  /** Executes the given python module as a new process and returns the exit code along with the stdout lines
    * @param pythonModule
    * @return
    */
  def callPython(pythonModule: String): (Int, List[String]) = {
    val wd = os.pwd / os.up
    val invoked = os.proc("python3", "-m", pythonModule).call(cwd = wd)
    (invoked.exitCode, invoked.out.lines.toList)
  }

  /** Executes Crosshair check on the given python (i.e., .py) file from the given working directoy and with the given timeout in seconds.
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
