package ch.epfl.web.service.main.simulator

trait SimulatorInterface {

  /** Start a new simulation session with the given devices and bindings.
    * Start the simulator in SVSHI mode and it then waits on SVSHI connection.
    * When the function returns, the simulator is ready for SVSHI connection.
    * @param physicalStructureJson
    * @param appBindingsJson
    * @param groupAddressToPhysIdJson
    */
  def startSimulate(physicalStructureJson: String, appBindingsJson: String, groupAddressToPhysId: Map[String, Int])(debug: String => Unit = s => ()): Unit

  /** Stop the simulator
    */
  def stop()(debug: String => Unit = s => ()): Unit

  /** Return true if the simulator is running, false otherwise
    * @return
    */
  def isRunning(): Boolean

  /** Return the ip addr of the simulator for running svshi on it
    *
    * @return
    */
  def getIpAddr(): String
}
