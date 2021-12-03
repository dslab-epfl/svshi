package ch.epfl.core.verifier.static.python

object ProcRunner {
  def callPython(pythonModule: String): (Int, List[String]) = {
    val wd = os.pwd / os.up
    val invoked = os.proc("python3", "-m", pythonModule).call(cwd = wd)
    (invoked.exitCode, invoked.out.lines.toList)
  }

  def callCrosshair(filePath: String, wd: os.Path, perConditionTimeoutSeconds: Int): (Int, List[String]) = {
    val invoked = os.proc("crosshair", "check", filePath, "--report_all", "--per_condition_timeout",  s"$perConditionTimeoutSeconds").call(cwd = wd, check = false)
    (invoked.exitCode, invoked.out.lines.toList ++ invoked.err.lines.toList)
  }
}
