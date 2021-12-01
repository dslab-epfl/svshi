package ch.epfl.core.verifier.static.python

object ProcRunner {
  def callPython(pythonModule: String): (Int, List[String]) = {
    val wd = os.pwd / os.up
    val invoked = os.proc("python3", "-m", pythonModule).call(cwd = wd)
    (invoked.exitCode, invoked.out.lines.toList)
  }

  def callCrosshair(filePath: String, perConditionTimeoutSeconds: Int): (Int, List[String]) = {
    val wd = os.pwd / os.up
    val invoked = os.proc("crosshair", "check", filePath, s"--per_condition_timeout $perConditionTimeoutSeconds").call(cwd = wd)
    (invoked.exitCode, invoked.out.lines.toList)
  }
}
