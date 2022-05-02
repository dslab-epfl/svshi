package ch.epfl.core.utils.subprocess

trait SvshiSubProcess {

  /** Kill the subprocess by forcing its destruction
    */
  def forceStop(): Unit

  /** Return a Boolean indicating whether the subprocess is still alive or not
    * @return
    */
  def isAlive: Boolean

  /** Return the exit code as an Option: if the process has terminated, the return code is wrapped
    * in an Option, else the function returns None
    * @return
    */
  def exitCode: Option[Int]

}
