package ch.epfl.web.service.main.session

import ch.epfl.web.service.main.session.applications.Application
import ch.epfl.web.service.main.session.floors.Floor
import ch.epfl.web.service.main.svshi.SvshiInterface
import ch.epfl.web.service.main.utils.{Constants, FileUtils}
import os.Path

import java.time.Instant
import scala.collection.mutable

class Session(val id: String) {

  private val SVSHI_ADDR_DOCKER = "svshi_container"
  private val SVSHI_ADDR = "127.0.0.1"
  val SVSHI_PORT = 4242
  private val SIMULATOR_ADDR_DOCKER = "simulator_container"
  private val SIMULATOR_ADDR = "127.0.0.1"
  val SIMULATOR_PORT = 4646
  val SIMULATOR_KNX_PORT = 3671

  val privateFolderPath: Path = Constants.SESSIONS_FOLDER_PATH / id
  os.makeDir.all(privateFolderPath)

  val floorFolder: Path = privateFolderPath / "floors"
  os.makeDir.all(privateFolderPath)

  private val etsFileName = "etsProjFile.knxproj"
  val etsFileFolder: Path = privateFolderPath / "ets"
  os.makeDir.all(etsFileFolder)
  val etsFilePath: Path = etsFileFolder / etsFileName

  private val parsedPhysicalStructureJsonFolderName = "physicalStructure"
  private val parsedPhysicalStructureJsonFolderPath = privateFolderPath / parsedPhysicalStructureJsonFolderName
  os.makeDir.all(parsedPhysicalStructureJsonFolderPath)
  private val parsedPhysicalStructureJsonFileName = "physicalStructure.json"
  private val parsedPhysicalStructureJsonFilePath = parsedPhysicalStructureJsonFolderPath / parsedPhysicalStructureJsonFileName

  private val deviceMappingsFileName = "deviceMappings.json"
  private val deviceMappingsFolderPath: Path = privateFolderPath / "mappings"
  os.makeDir.all(deviceMappingsFolderPath)
  private val deviceMappingsFilePath: Path = deviceMappingsFolderPath / deviceMappingsFileName

  private val applicationsFolderPath: Path = privateFolderPath / "applications"
  os.makeDir.all(applicationsFolderPath)

  private val floorMap = mutable.Map[Int, Floor]()
  private val appMap = mutable.Map[String, Application]()

  private var expireDate: Instant = Instant.now().plusSeconds(Constants.SESSION_LIFESPAN_SECONDS)
  // To add data for a session like devices, svshi instance, ...

  /** Return the address of SVSHI to use for this session, given whether this service runs in docker mode or not
    * @param docker
    * @return
    */
  def getSvshiAddr(docker: Boolean): String = if (docker) SVSHI_ADDR_DOCKER else SVSHI_ADDR

  /** Return the address of the KNX simulator to use for this session, given whether this service runs in docker mode or not
    * @param docker
    * @return
    */
  def getSimulatorAddr(docker: Boolean): String = if (docker) SIMULATOR_ADDR_DOCKER else SIMULATOR_ADDR

  /** Return the device Mappings for the current ETS file. If the mappings are not defined yet, send a request to the SVSHIInterface
    * to get them computed and set them. Throws exception if the ETS file is not yet defined or if SVSHI does not reply or replies
    * with an error
    * @param svshi: An instance of an interface to SVSHI
    * @return
    */
  def getOrComputeDeviceMappings(svshi: SvshiInterface): String = {
    if (!hasEtsFile) {
      throw new IllegalStateException("The ETS file must be set to compute Device Mappings!")
    }
    if (!hasMappings) {
      val mappingsJson = svshi.getDeviceMappings(etsFilePath)
      // Store the mappings
      FileUtils.writeToFileOverwrite(deviceMappingsFilePath, mappingsJson.getBytes)
    }

    FileUtils.readFileContentAsString(deviceMappingsFilePath)
  }

  private def hasMappings = {
    os.exists(deviceMappingsFilePath) && os.isFile(deviceMappingsFilePath)
  }

  def getOrComputePhysicalStructureJson(svshi: SvshiInterface): String = {
    if (!hasEtsFile) {
      throw new IllegalStateException("The ETS file must be set to compute Device Mappings!")
    }
    if (!hasParsedPhysicalStructureJson) {
      val parsedJson = svshi.getParserPhysicalStructureJson(getEtsFileZip().get)
      FileUtils.writeToFileOverwrite(parsedPhysicalStructureJsonFilePath, parsedJson.getBytes)
    }
    FileUtils.readFileContentAsString(parsedPhysicalStructureJsonFilePath)
  }
  private def hasParsedPhysicalStructureJson = {
    os.exists(parsedPhysicalStructureJsonFilePath) && os.isFile(parsedPhysicalStructureJsonFilePath)
  }

  /** Add a new app to the session. If it already exists, it is replaced
    * @param appName
    * @param newAppFolderPath
    */
  def addApp(appName: String, newAppFolderPath: os.Path): Unit = {
    if (!os.isDir(newAppFolderPath)) throw new IllegalArgumentException("The applicationFolderPath must point to a directory containing the application's files!")

    FileUtils.copyDirWithNewName(newAppFolderPath, applicationsFolderPath, appName)
    appMap += (appName -> Application(appName, applicationsFolderPath / appName))
  }

  /** Delete the app with the given name from the session
    * @param appName
    */
  def removeApp(appName: String) = {
    if (appMap.contains(appName)) {
      FileUtils.deleteIfExists(appMap(appName).folderPath)
      appMap -= appName
    }
  }

  /** Return the list of application names currently in the session
    * @return
    */
  def appNamesList: List[String] = appMap.keys.toList

  def getZipForAppFiles(appName: String): Option[Array[Byte]] = {
    appMap.get(appName) match {
      case Some(app) =>
        FileUtils.zipInMem(List(app.folderPath))
      case None => None
    }
  }

  /** Adds a new floor to the session
    *
    * @param floorNumber
    * @param floorName
    * @param blueprintPath
    */
  def addFloor(floorNumber: Int, floorName: String, blueprintPath: os.Path): Unit = {
    val newBlueprintPath = constructFloorFilePath(floorNumber, floorName)
    os.copy(blueprintPath, newBlueprintPath, replaceExisting = true)
    val newFloor = Floor(floorNumber, floorName, newBlueprintPath)
    floorMap += (floorNumber -> newFloor)

  }

  /** Remove the floor with the given number, removing files as well
    * @param floorNumber
    */
  def removeFloor(floorNumber: Int): Unit = {
    val folder = constructFloorFolderPath(floorNumber)
    if (os.exists(folder)) os.remove.all(folder)
    floorMap -= floorNumber
  }

  /** Return the list of floors of that session
    * @return
    */
  def getFloors(): List[Floor] = {
    this.floorMap.values.toList
  }

  def getFloor(id: Int): Option[Floor] = {
    this.floorMap.get(id)
  }

  /** Set or replace the etsFile by copying the given one in the private session's files
    * @param newEtsFilePath
    */
  def setEtsFile(newEtsFilePath: os.Path) = {
    if (os.exists(newEtsFilePath)) {
      os.copy(newEtsFilePath, etsFilePath, replaceExisting = true)
      os.remove(deviceMappingsFilePath)
      os.remove(parsedPhysicalStructureJsonFilePath)
    }
  }

  /** Return an optional array of bytes that is an zip archive containing the ETS file
    * @return an optional array of bytes that is an zip archive containing the ETS file
    */
  def getEtsFileZip(): Option[Array[Byte]] = FileUtils.zipInMem(List(etsFilePath))

  def hasEtsFile: Boolean = os.exists(this.etsFilePath) && os.isFile(this.etsFilePath)

  /** Set the expire date to the new one
    */
  def setExpiration(expireDate: Instant): Unit = {
    this.expireDate = expireDate
  }

  /** Set the expiration date at now + Constants.SESSION_LIFESPAN_SECONDS
    */
  def renewLifespan(): Unit = {
    this.expireDate = Instant.now().plusSeconds(Constants.SESSION_LIFESPAN_SECONDS)
  }

  /** Returns true if the Session is expired
    */
  def isExpired: Boolean = {
    expireDate.isBefore(Instant.now())
  }

  /** Delete the whole session files and created resources
    * Reference to this instance must then be deleted
    */
  def delete(): Unit = {
    os.remove.all(privateFolderPath)
  }

  def getExpirationDate: Instant = expireDate

  def backupToFile(folderPath: os.Path): Unit = {
    // For now, everything is in files already
  }

  def reloadFromFile(): Unit = {
    floorMap.clear()
    floorMap ++= createFloorMapFromFile()
    appMap.clear()
    appMap ++= createAppMapFromFile()

  }

  private def createFloorMapFromFile(): Map[Int, Floor] = {
    val floorNumbers = FileUtils.getListOfFolders(floorFolder).map(f => f.segments.toList.last.toInt)
    val floorNumbersNames: List[(Int, String, Path)] =
      floorNumbers.map(i => (i, FileUtils.getListOfFiles(floorFolder / s"$i").head)).map(t => (t._1, t._2.segments.toList.last.replace(".dxf", ""), t._2)).toList
    floorNumbersNames.map(t => (t._1, Floor(t._1, t._2, t._3))).toMap
  }

  private def createAppMapFromFile(): Map[String, Application] = {
    val appNames: List[String] = FileUtils.getListOfFolders(applicationsFolderPath).map(p => p.segments.toList.last)
    appNames.map(name => name -> Application(name, applicationsFolderPath / name)).toMap
  }

  private def constructFloorFolderPath(floorNumber: Int): os.Path = {
    val folder = floorFolder / s"$floorNumber"
    os.makeDir.all(folder)
    folder
  }

  private def constructFloorFilePath(floorNumber: Int, floorName: String): os.Path = {
    val folder = constructFloorFolderPath(floorNumber)
    folder / s"$floorName.dxf"
  }
}
