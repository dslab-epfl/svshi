package ch.epfl.web.service.main.simulator

import upickle.default._
case class SimulatorHttp(simulatorAddress: String, simulatorPort: Int) extends SimulatorInterface {

  val READ_SIMULATOR_REQUEST_TIMEOUT = 600_000 // Milliseconds
  /** Start a new simulation session with the given devices and bindings.
    * Start the simulator in SVSHI mode and it then waits on SVSHI connection.
    * When the function returns, the simulator is ready for SVSHI connection.
    *
    * @param physicalStructureJson
    * @param appBindingsJson
    * @param groupAddressToPhysIdJson
    */
  override def startSimulate(physicalStructureJson: String, appBindingsJson: String, groupAddressToPhysId: Map[String, Int])(debug: String => Unit = s => ()): Unit = {
    val gaToPhysIdJson = write(groupAddressToPhysId, indent = 2)
    val body =
      s"""{
        |  "physicalStructure": $physicalStructureJson,
        |  "appBindings": $appBindingsJson,
        |  "gaToPhysId": $gaToPhysIdJson
        |}""".stripMargin
    debug(s"Send /simulator/config request with body = \n$body")
    val rConfig =
      requests.post(
        s"http://$simulatorAddress:$simulatorPort/simulator/config",
        data = body,
        headers = Map("Content-Type" -> "application/json"),
        check = false,
        readTimeout = READ_SIMULATOR_REQUEST_TIMEOUT
      )
    if (rConfig.statusCode != 200) throw new RuntimeException(s"Cannot generate the config on the simulator! See error:\n${rConfig.text()}")

    // Config is ready on simulator side
    debug(s"Send $simulatorAddress:$simulatorPort/simulator/start request")
    val rStart = requests.post(s"http://$simulatorAddress:$simulatorPort/simulator/start", check = false, readTimeout = READ_SIMULATOR_REQUEST_TIMEOUT)
    if (rStart.statusCode != 200) throw new RuntimeException(s"Cannot start the simulator! See error:\n${rStart.text()}")
  }

  /** Stop the simulator
    */
  override def stop()(debug: String => Unit = s => ()): Unit = {
    debug(s"Send $simulatorAddress:$simulatorPort/simulator/stop request")
    val rStop = requests.post(s"http://$simulatorAddress:$simulatorPort/simulator/stop", check = false, readTimeout = READ_SIMULATOR_REQUEST_TIMEOUT)
    if (rStop.statusCode != 200) throw new RuntimeException(s"Cannot stop the simulator! See error:\n${rStop.text()}")
  }

  /** Return true if the simulator is running, false otherwise
    *
    * @return
    */
  override def isRunning(): Boolean = {

    val r = requests.get(s"http://$simulatorAddress:$simulatorPort/simulator/running", check = false, readTimeout = READ_SIMULATOR_REQUEST_TIMEOUT)
    if (r.statusCode != 200) throw new RuntimeException(s"Cannot get the state of the simulator! See error:\n${r.text()}")
    val mapRes = read[Map[String, Boolean]](r.text())
    mapRes("isRunning")
  }

  /** Return the ip addr of the simulator for running svshi on it
    *
    * @return
    */
  override def getIpAddr(): String = {
    val r = requests.get(s"http://$simulatorAddress:$simulatorPort/simulator/ipAddr", check = false, readTimeout = READ_SIMULATOR_REQUEST_TIMEOUT)
    if (r.statusCode != 200) throw new RuntimeException(s"Cannot get the IP addr of the simulator! See error:\n${r.text()}")
    val mapRes = read[Map[String, String]](r.text())
    mapRes("address")
  }
}
