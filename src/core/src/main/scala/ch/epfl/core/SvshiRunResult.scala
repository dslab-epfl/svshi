package ch.epfl.core

import ch.epfl.core.utils.subprocess.SvshiSubProcess

/** Decorate the SvshiSubProcess to be used even with blocking call, by returning hardcoded values
  * or interact with the subprocess if non-blocking mode
  * @param svshiSubProcess
  * @param exitCode
  */
class SvshiRunResult(private val svshiSubProcess: Option[SvshiSubProcess], private val hardCodedExitCode: Int) extends SvshiSubProcess {

  /** Force stop the Svshi command
    * if this instance was created by a blocking call, this function will do nothing
    */
  def forceStop(): Unit = if (svshiSubProcess.isDefined) svshiSubProcess.get.forceStop()

  /** Return the exit code
    * If the Svshi run command is still running, it returns None.
    * @return
    */
  def exitCode: Option[Int] = if (svshiSubProcess.isDefined) svshiSubProcess.get.exitCode else Some(hardCodedExitCode)

  /** Return a Boolean indicating whether the subprocess is still alive or not
    *
    * @return
    */
  override def isAlive: Boolean = if (svshiSubProcess.isDefined) svshiSubProcess.get.isAlive else false
}
