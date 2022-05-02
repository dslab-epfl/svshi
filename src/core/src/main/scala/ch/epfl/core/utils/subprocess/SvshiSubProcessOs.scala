package ch.epfl.core.utils.subprocess

import scala.util.Try

class SvshiSubProcessOs(subProcess: os.SubProcess) extends SvshiSubProcess {

  /** Kill the subprocess by forcing its destruction
    */
  override def forceStop(): Unit = subProcess.destroyForcibly()

  /** Return a Boolean indicating whether the subprocess is still alive or not
    *
    * @return
    */
  override def isAlive: Boolean = subProcess.isAlive()

  /** Return the exit code as an Option: if the process has terminated, the return code is wrapped
    * in an Option, else the function returns None
    *
    * @return
    */
  override def exitCode: Option[Int] = Try(Some(subProcess.exitCode())).getOrElse(None)
}
