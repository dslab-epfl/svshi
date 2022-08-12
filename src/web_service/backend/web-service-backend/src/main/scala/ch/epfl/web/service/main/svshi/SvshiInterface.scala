package ch.epfl.web.service.main.svshi

trait SvshiInterface {

  /** Get the device mappings from SVSHI for the given etsProjFile, returns the json answer as string
    * @param etsProjFile
    * @return
    */
  def getDeviceMappings(etsProjFile: os.Path): String

  /** Get the parsed physicalStructure from SVSHI and return the json in a string
    * @param etsFileZip The ETS file in a zip archive to be parsed
    * @return the json of the physicalStructure as a string
    */
  def getParserPhysicalStructureJson(etsFileZip: Array[Byte]): String

  /** Return the AppBindings from the appLibrary
    * Return the empty string if no appBindings are in the library (i.e., no apps are installed)
    * @return the appBindings.json file content in a string
    */
  def getAppLibraryBindings(): String

  /** Return the mapping from GA to physicalId from SVSHI's assignments
    * Returns the map returned by SVSHI as a scala map. Returns an empty map if no assignments are present on SVSHI
    * @return
    */
  def getAssignmentGaToPhysId(): Map[String, Int]

  /** Generate a new app using SVSHI and returns the new app in a zip as an array of bytes.
    * Does not let any state on the SVSHI instance: no new app in the generated folder (as before), no new installed apps
    * @param appName the name of the app to generate
    * @param jsonProto the prototypical structure json as a String
    * @return a zip archive containing the new app
    */
  def generateApp(appName: String, jsonProto: String): Array[Byte]

  /** Get the list of names of installedApps on SVSHI
    * @return a List containing the names of the apps
    */
  def getInstalledAppNames(): List[String]

  /** Uninstall all apps currently installed on SVSHI
    * @return a tuple containing a boolean indicating whether the operation was successful and a list of strings containing
    *         the messages from SVSHI
    */
  def uninstallAllApps(): (Boolean, List[String])

  /** Add the content of the zip archive to generated folder of SVSHI
    * @param zipArchive
    */
  def addToGenerated(zipArchive: Array[Byte]): Unit

  /** Remove everything from the generated folder of SVSHI
    */
  def removeAllFromGenerated(): Unit

  /** Generate the bindings on SVSHI using the provided ets file archive
    * @param etsFileAsZip
    * @return a tuple containing a boolean indicating whether the operation was successful and a list of strings containing
    *         the messages from SVSHI
    */
  def generateBindings(etsFileAsZip: Array[Byte]): (Boolean, List[String])

  /** Compile the apps on SVSHI using the provided ets file archive
    * @param etsFileAsZip
    * @return a tuple containing a boolean indicating whether the operation was successful and a list of strings containing
    *         the messages from SVSHI
    */
  def compile(etsFileAsZip: Array[Byte]): (Boolean, List[String])

  /** Start SVSHI run command with the given ip and port
    * @param ip
    * @param port
    */
  def run(ip: String, port: Int)(debug: String => Unit = s => ()): Unit

  /** Stop SVSHI run command with the given ip and port
    */
  def stop(): Unit

  /** Return a boolean indicating whether SVSHI is running or not with a knx system
    * @return Boolean true if SVSHI is currently running, false otherwise
    */
  def getRunStatus(): Boolean

  /** Get the run logs from SVSHI and return them as a list of lines
    * @return the lines of the logs in a List
    */
  def getRunLogs(): List[String]

  /** Get the received telegrams logs from SVSHI and return them as a list of lines
    * @return the lines of the logs in a List
    */
  def getReceivedTelegramsLogs(): List[String]

  /** Get the execution logs from SVSHI and return them as a list of lines
    * @return the lines of the logs in a List
    */
  def getExecutionLogs(): List[String]

}
