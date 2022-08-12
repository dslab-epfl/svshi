package ch.epfl.web.service.main

import ch.epfl.web.service.main.CustomMatchers.{beAFile, containThePairOfHeaders, existInFilesystem, haveSameContentAsIgnoringBlanks}
import ch.epfl.web.service.main.TestUtils.compareFolders
import ch.epfl.web.service.main.mocks.{SimulatorHttpServerMock, SvshiHttpServerMock}
import ch.epfl.web.service.main.session.SessionManager
import ch.epfl.web.service.main.session.floors.{FloorJson, FloorListJson}
import ch.epfl.web.service.main.simulator.{SimulatorHttp, SimulatorInterface}
import ch.epfl.web.service.main.svshi.{SvshiHttp, SvshiInterface}
import ch.epfl.web.service.main.utils.{Constants, FileUtils}
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import upickle.default._

import scala.language.postfixOps

class E2eTests extends AnyFlatSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with TimeLimitedTests {
  def timeLimit: Span = 30 seconds
  private val requestsReadTimeout = 20_000 // Milliseconds

  private val resTestFolderPath = os.pwd / "res" / "test"
  private val resMocksFolderPath = os.pwd / "res" / "mocks"

  private val svshiAddr = "127.0.0.1"
  private val svshiPort = 4242
  private val svshiMock = SvshiHttpServerMock(host = svshiAddr, port = svshiPort)
  private val svshiInterfaceMock = SvshiHttp(svshiAddress = svshiAddr, svshiPort = svshiPort)
  private val simulatorAddr = "127.0.0.1"
  private val simulatorPort = 4646
  private val simulatorMock = SimulatorHttpServerMock(host = simulatorAddr, port = simulatorPort)
  private val simulatorInterfaceMock = SimulatorHttp(simulatorAddress = simulatorAddr, simulatorPort = simulatorPort)

  case class MockFactory() extends SvshiSimInterfaceFactory {
    override def getSvshiHttpInterface(ip: String, port: Int): SvshiInterface = svshiInterfaceMock
    override def getSimulatorHttpInterface(ip: String, port: Int): SimulatorInterface = simulatorInterfaceMock
  }

  private val apiServer = ApiServer(MockFactory())

  private val expectedHeaders = Seq("Access-Control-Allow-Origin".toLowerCase -> "*")
  private val testCadFilePath = resTestFolderPath / "test_cad_file.dxf"
  private lazy val testCadFileZip = FileUtils.zipInMem(List(testCadFilePath)).get
  private val testCadFilePath2 = resTestFolderPath / "test_cad_file_2.dxf"
  private lazy val testCadFileZip2 = FileUtils.zipInMem(List((testCadFilePath2))).get
  private val dslabProtoEtsFilePath = resTestFolderPath / "DSLAB_proto.knxproj"
  private lazy val dslabProtoEtsFileZip = FileUtils.zipInMem(List((dslabProtoEtsFilePath))).get
  private val testProtoEtsFilePath = resTestFolderPath / "ets_project_test.knxproj"
  private lazy val testProtoEtsFileZip = FileUtils.zipInMem(List((testProtoEtsFilePath))).get

  private val backupWebserviceHome = os.pwd / "res" / "backup_web_service_home"

  override def beforeEach(): Unit = {
    SessionManager.deleteCurrentSession()
    if (os.exists(Constants.SVSHI_WEBSERVICE_HOME)) os.remove.all(Constants.SVSHI_WEBSERVICE_HOME)
    svshiMock.resetMock()
    simulatorMock.resetMock()
    svshiMock.start()
    simulatorMock.start()
  }
  override def beforeAll(): Unit = {
    if (os.exists(backupWebserviceHome)) os.remove.all(backupWebserviceHome)

    if (os.exists(Constants.SVSHI_WEBSERVICE_HOME)) os.copy(Constants.SVSHI_WEBSERVICE_HOME, backupWebserviceHome)

    apiServer.start()

  }
  override def afterEach(): Unit = {
    svshiMock.stop()
    simulatorMock.stop()
  }
  override def afterAll(): Unit = {
    if (os.exists(Constants.SVSHI_WEBSERVICE_HOME)) os.remove.all(Constants.SVSHI_WEBSERVICE_HOME)

    if (os.exists(backupWebserviceHome)) os.copy(backupWebserviceHome, Constants.SVSHI_WEBSERVICE_HOME)
    if (os.exists(backupWebserviceHome)) os.remove.all(backupWebserviceHome)

    if (os.exists(Constants.TEMP_FOLDER_PATH)) os.remove.all(Constants.TEMP_FOLDER_PATH)
  }

  "getVersion endpoint" should "respond code 200" in {
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/version", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
  }

  "get sessionId endpoint" should "create a new session if does not exist" in {
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/sessionId", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    val sessionOpt = SessionManager.getCurrentSession
    sessionOpt shouldBe defined
  }

  "get sessionId endpoint" should "load session from file if files exist" in {
    SessionManager.getCurrentSession shouldBe None

    val sessionPath = Constants.SESSIONS_FOLDER_PATH / "static_session_id"
    os.makeDir.all(sessionPath)
    os.makeDir.all(sessionPath / "ets")
    os.copy(resTestFolderPath / "DSLAB_proto.knxproj", sessionPath / "ets" / "etsProjFile.knxproj")

    os.makeDir.all(sessionPath / "floors" / "0")
    os.copy(resTestFolderPath / "test_cad_file.dxf", sessionPath / "floors" / "0" / "floor 0.dxf")

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/sessionId", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    val sessionOpt = SessionManager.getCurrentSession
    sessionOpt shouldBe defined

    val session = sessionOpt.get
    os.read.bytes(session.etsFilePath) shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")

    session.getFloor(0) shouldBe defined
    session.getFloor(0).get.number shouldEqual 0
    session.getFloor(0).get.name shouldEqual "floor 0"
    os.read.bytes(session.getFloor(0).get.blueprintFilePath) shouldEqual os.read.bytes(resTestFolderPath / "test_cad_file.dxf")

  }

  "get sessionId endpoint" should "create a fresh session if files do not exist" in {
    SessionManager.getCurrentSession shouldBe None

    val sessionPath = Constants.SESSIONS_FOLDER_PATH / "static_session_id"
    os.remove.all(sessionPath)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/sessionId", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    val sessionOpt = SessionManager.getCurrentSession
    sessionOpt shouldBe defined

    val session = sessionOpt.get
    session.etsFilePath shouldNot existInFilesystem

    session.getFloor(0) shouldBe None
  }

  "post addFloor" should "add the floor to the session" in {
    val session = SessionManager.createNewSession()
    val floorNumber = 0
    val floorName = "floor-name-4242"
    val data = testCadFileZip
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName/$floorNumber",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r.statusCode shouldEqual 200

    session.getFloors().length shouldEqual 1
    val floor = session.getFloors().head
    floor.number shouldEqual floorNumber
    floor.name shouldEqual floorName
    floor.blueprintFilePath should existInFilesystem
    floor.blueprintFilePath should haveSameContentAsIgnoringBlanks(testCadFilePath)
  }

  "post addFloor" should "add the floor to the session when request sent twice" in {
    val session = SessionManager.createNewSession()
    val floorNumber = 0
    val floorName = "floor-name-4242"
    val data = testCadFileZip
    requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName/$floorNumber",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName/$floorNumber",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r.statusCode shouldEqual 200

    session.getFloors().length shouldEqual 1
    val floor = session.getFloors().head
    floor.number shouldEqual floorNumber
    floor.name shouldEqual floorName
    floor.blueprintFilePath should existInFilesystem
    floor.blueprintFilePath should haveSameContentAsIgnoringBlanks(testCadFilePath)
  }

  "post addFloor" should "replace the floor if called with new parameters for the same number" in {
    val session = SessionManager.createNewSession()
    val floorNumber = 0
    val floorName1 = "floor-name-4242"
    val data1 = testCadFileZip
    requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName1/$floorNumber",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data1
    )
    val floorName2 = "floor-name-new"
    val data2 = testCadFileZip2
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName2/$floorNumber",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data2
    )
    r.statusCode shouldEqual 200

    session.getFloors().length shouldEqual 1
    val floor = session.getFloors().head
    floor.number shouldEqual floorNumber
    floor.name shouldEqual floorName2
    floor.blueprintFilePath should existInFilesystem
    floor.blueprintFilePath should haveSameContentAsIgnoringBlanks(testCadFilePath2)
  }

  "post addFloor" should "add the floor to the session and already has a floor" in {
    val session = SessionManager.createNewSession()
    val floorNumber1 = 0
    val floorName1 = "floor-name-4242"
    val data1 = testCadFileZip
    requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName1/$floorNumber1",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data1
    )

    val floorNumber2 = 1
    val floorName2 = "floor-name-4242"
    val data2 = testCadFileZip2

    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/addFloor/$floorName2/$floorNumber2",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data2
    )
    r.statusCode shouldEqual 200

    session.getFloors().length shouldEqual 2
    val floor1 = session.getFloors().filter(f => f.number == floorNumber1).head
    floor1.number shouldEqual floorNumber1
    floor1.name shouldEqual floorName1
    floor1.blueprintFilePath should existInFilesystem
    floor1.blueprintFilePath should haveSameContentAsIgnoringBlanks(testCadFilePath)

    val floor2 = session.getFloors().filter(f => f.number == floorNumber2).head
    floor2.number shouldEqual floorNumber2
    floor2.name shouldEqual floorName2
    floor2.blueprintFilePath should existInFilesystem
    floor2.blueprintFilePath should haveSameContentAsIgnoringBlanks(testCadFilePath2)
  }

  "post etsFile" should "set the file of the session" in {
    val session = SessionManager.createNewSession()
    val data = testProtoEtsFileZip
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r.statusCode shouldEqual 200
    session.etsFilePath should existInFilesystem
    os.read.bytes(session.etsFilePath) shouldEqual os.read.bytes(testProtoEtsFilePath)
  }

  "post etsFile" should "replace the file of the session" in {
    val session = SessionManager.createNewSession()
    val data = testProtoEtsFileZip
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r.statusCode shouldEqual 200

    val data2 = dslabProtoEtsFileZip
    val r2 = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data2
    )
    r2.statusCode shouldEqual 200
    session.etsFilePath should existInFilesystem
    os.read.bytes(session.etsFilePath) shouldEqual os.read.bytes(dslabProtoEtsFilePath)
  }

  "post reset/session" should "delete the session especially removing its files" in {
    val session = SessionManager.createNewSession()
    val data = testProtoEtsFileZip
    val r = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r.statusCode shouldEqual 200
    session.etsFilePath should existInFilesystem

    val etsSessionPath = session.etsFilePath
    val privateFolderPath = session.privateFolderPath
    val r1 = requests.post(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/reset/session",
      check = false,
      readTimeout = requestsReadTimeout,
      data = data
    )
    r1.statusCode shouldEqual 200
    r1.text() should include("successfully erased!")
    SessionManager.getCurrentSession shouldEqual None

    etsSessionPath shouldNot existInFilesystem
    privateFolderPath shouldNot existInFilesystem
  }

  "get floors" should "return an empty list when the session has no floors" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/floors", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    val floors = FloorListJson.from(r.text())
    floors.floors shouldBe empty

  }

  "get floors" should "return the correct json when the session has floors" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "test_cad_file.dxf"
    session.addFloor(0, "floor1", file)
    session.addFloor(1, "floor2", file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/floors", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    val floors = FloorListJson.from(r.text())
    floors.floors should contain only (FloorJson(0, "floor1"), FloorJson(1, "floor2"))

  }

  "get floor file" should "return 404 when the number does not correspond to any floor" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "test_cad_file.dxf"
    session.addFloor(0, "floor1", file)
    session.addFloor(1, "floor2", file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/floorFile/42", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 404
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
  }

  "get floor file" should "return 200 and the correct file when the number exists" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "test_cad_file.dxf"
    session.addFloor(0, "floor1", file)
    session.addFloor(1, "floor2", file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/floorFile/1", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.data.array shouldEqual os.read.bytes(file)
  }

  "get ets file" should "return 404 when not set" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 404
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
  }

  "get ets file" should "return 200 and the correct file when the number exists" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/etsFile", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.data.array shouldEqual os.read.bytes(file)
  }

  "get getDeviceMappings" should "return the correct json content when ETS Proj file is defined in the session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/getDeviceMappings", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    val expectedFile = resMocksFolderPath / "available_proto_devices.json"
    r.text() shouldEqual FileUtils.readFileContentAsString(expectedFile)
  }

  "get getDeviceMappings" should "return the correct json content when ETS Proj file is defined in the session and called twice" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)

    val r1 = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/getDeviceMappings", check = false, readTimeout = requestsReadTimeout)
    r1.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/getDeviceMappings", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    val expectedFile = resMocksFolderPath / "available_proto_devices.json"
    r.text() shouldEqual FileUtils.readFileContentAsString(expectedFile)
  }

  "get getDeviceMappings" should "reply an error code 412 when the ets proj file is not defined in the session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/getDeviceMappings", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 412
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.text() should include("No ETS project file set for this session!")
  }

  "get getDeviceMappings" should "reply an error code 500 when the ets proj file is defined in the session but svshi is down" in {
    svshiMock.stop()
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/getDeviceMappings", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.text() should include("An internal error occurred while getting the devices mappings!")
  }

  "generateApp" should "reply with an app zip when svshi works properly" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName = "app_one"
    val jsonProtoString = os.read(resMocksFolderPath / "app_one_generated" / "app_prototypical_structure.json")
    val r = requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/generateApp/$appName", check = false, data = jsonProtoString, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.bytes shouldEqual FileUtils.zipInMem(List(resMocksFolderPath / "app_one_generated")).get

  }

  "get application/names" should "return the list of apps of the session as a json array" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    session.addApp("app_one", resMocksFolderPath / "app_one_generated")
    session.addApp("app_two", resMocksFolderPath / "app_one_generated")
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/names/", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    read[List[String]](r.text()) shouldEqual List("app_one", "app_two")
  }

  "get application/names" should "return empty json array when no apps" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/names/", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    read[List[String]](r.text()) shouldEqual Nil
  }

  "get applications/files/:appName" should "reply with 404 when appName not in session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"
    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/files/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 404
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.bytes shouldEqual Array[Byte]()
  }

  "get applications/files/:appName" should "reply with 200 and the zip when appName in session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"
    session.addApp("app_one", resMocksFolderPath / "app_one_generated")

    val r = requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/files/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    FileUtils.copyDirWithNewName(resMocksFolderPath / "app_one_generated", Constants.TEMP_FOLDER_PATH, "app_one")
    FileUtils.writeToFileOverwrite(Constants.TEMP_FOLDER_PATH / "received_zip.zip", r.bytes)
    FileUtils.unzip(Constants.TEMP_FOLDER_PATH / "received_zip.zip", Constants.TEMP_FOLDER_PATH / "output_unzip")
    Constants.TEMP_FOLDER_PATH / "output_unzip" / "app_one" should existInFilesystem
    Constants.TEMP_FOLDER_PATH / "output_unzip" / "app_one" shouldNot beAFile
    compareFolders(Constants.TEMP_FOLDER_PATH / "output_unzip" / "app_one", Constants.TEMP_FOLDER_PATH / "app_one", TestUtils.defaultIgnoredFilesAndDir)
  }

  "post /applications/add/:appName" should "add the app when the zip is valid with a folder at the root" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"

    val zip = FileUtils.zipInMem(List(resMocksFolderPath / "app_one_generated")).get
    val r = requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/add/$appName", check = false, data = zip, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    session.appNamesList should contain only appName

    FileUtils.copyDirWithNewName(resMocksFolderPath / "app_one_generated", Constants.TEMP_FOLDER_PATH, appName)

    session.getZipForAppFiles(appName) shouldBe defined
    val receivedZip = session.getZipForAppFiles(appName).get
    os.write(Constants.TEMP_FOLDER_PATH / "zip.zip", receivedZip)

    val unzipped = Constants.TEMP_FOLDER_PATH / "unzipped"
    FileUtils.unzip(Constants.TEMP_FOLDER_PATH / "zip.zip", unzipped)

    unzipped / appName should existInFilesystem
    TestUtils.compareFolders(unzipped / appName, Constants.TEMP_FOLDER_PATH / appName, TestUtils.defaultIgnoredFilesAndDir)

    val expected = FileUtils.zipInMem(List(Constants.TEMP_FOLDER_PATH / "app_one")).get
    session.getZipForAppFiles(appName) shouldBe defined
    session.getZipForAppFiles(appName).get shouldEqual expected
  }

  "post /applications/add/:appName" should "add the app when the zip is valid with all files at the root" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"

    val zip = FileUtils.zipInMem(os.list(resMocksFolderPath / "app_one_generated").toList).get
    val r = requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/add/$appName", check = false, data = zip, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    session.appNamesList should contain only appName

    FileUtils.copyDirWithNewName(resMocksFolderPath / "app_one_generated", Constants.TEMP_FOLDER_PATH, "app_one")
    session.getZipForAppFiles(appName) shouldBe defined
    val receivedZip = session.getZipForAppFiles(appName).get
    os.write(Constants.TEMP_FOLDER_PATH / "zip.zip", receivedZip)

    val unzipped = Constants.TEMP_FOLDER_PATH / "unzipped"
    FileUtils.unzip(Constants.TEMP_FOLDER_PATH / "zip.zip", unzipped)

    unzipped / appName should existInFilesystem
    TestUtils.compareFolders(unzipped / appName, Constants.TEMP_FOLDER_PATH / appName, TestUtils.defaultIgnoredFilesAndDir)

    val expected = FileUtils.zipInMem(List(Constants.TEMP_FOLDER_PATH / "app_one")).get
    session.getZipForAppFiles(appName) shouldBe defined
    session.getZipForAppFiles(appName).get shouldEqual expected
  }

  "post /applications/delete/:appName" should "delete the app" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"

    session.addApp(appName, resMocksFolderPath / "app_one_generated")

    val r = requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/delete/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    session.appNamesList shouldBe empty
    os.list(session.privateFolderPath / "applications") shouldBe empty

  }

  "post /applications/delete/:appName" should "not fail even if app is not in session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val appName = "app_one"

    session.addApp(appName, resMocksFolderPath / "app_one_generated")

    val r = requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/applications/delete/does-not-exist", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    session.appNamesList should contain only (appName)
    os.list(session.privateFolderPath / "applications").map(p => p.segments.toList.last) should contain(appName)

  }

  "get svshi/applications/installed/names" should "return with a json array with all app names" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    svshiMock.installedApps = List("app_one", "app_two")
    val r =
      requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/installed/names", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    read[Seq[String]](r.text()) shouldEqual List("app_one", "app_two")

  }

  "get svshi/applications/installed/names" should "reply 500 error is svshi is down" in {
    svshiMock.stop()
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val r =
      requests.get(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/installed/names", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
  }

  "post svshi/applications/uninstall" should "uninstall all apps on svshi" in {
    svshiMock.installedApps = List("app_one", "app_two", "app_three")
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val r =
      requests.post(f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/uninstall", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "install session apps that are passed through body of the request on SVSHI" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write(List(appName1, appName2)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    svshiMock.installedApps should contain only (appName1, appName2)
  }

  "post svshi/applications/install" should "install session apps that are passed through body of the request on SVSHI even if previous apps were installed on SVSHI" in {
    svshiMock.installedApps = List("old_app1")
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write(List(appName1, appName2)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    svshiMock.installedApps should contain only (appName1, appName2)
  }

  "post svshi/applications/install" should "reply with 400 if one app is not in the session" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write(List(appName1, appName2, "not_in_session")),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 400
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include("'not_in_session' is not in your session")
    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "reply with 500 and correct msgs if generate bindings returned error" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val genBindMsg = "This is a bindings generation error message\nfrom svshi"
    svshiMock.nextGenerationBindingError = Some(genBindMsg)
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write(List(appName1, appName2)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include(genBindMsg)
    r.text() should include("Cannot generate the bindings! See SVSHI error:\n")
    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "reply with 500 and correct msgs if compilation returned error" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val compilationErrMsg = "This is a compilation error message\nfrom svshi"
    svshiMock.nextVerificationError = Some(compilationErrMsg)
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write(List(appName1, appName2)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include(compilationErrMsg)
    r.text() should include("Cannot compile the applications! See SVSHI error:\n")
    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "reply with 400 and when no apps in the list in the body" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val compilationErrMsg = "This is a compilation error message\nfrom svshi"
    svshiMock.nextVerificationError = Some(compilationErrMsg)
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write[List[String]](List()),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 400
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include("No apps are given, cannot install!")
    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "reply with 400 and the message when gen Binding threw" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    svshiMock.nextGenBindingErrorCode = Some(400)
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include("Cannot generateBindings on SVSHI! Here is the error:")
    r.text() should include("GEN BINDINGS ERROR MESSAGE FROM SVSHI")
    svshiMock.installedApps shouldBe empty
  }

  "post svshi/applications/install" should "reply with 400 and the message when compile threw" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    svshiMock.nextCompileErrorCode = Some(400)
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/applications/install",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    r.statusCode shouldEqual 500
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.text() should include("Cannot compile on SVSHI! Here is the error:")
    r.text() should include("COMPILE ERROR MESSAGE FROM SVSHI")
    svshiMock.installedApps shouldBe empty
  }

  "post simulation/start" should "create the config on simulator, start simulator and start svshi" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulation/start",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val physStruct = session.getOrComputePhysicalStructureJson(svshiInterfaceMock)
    val appBindings = svshiInterfaceMock.getAppLibraryBindings()
    val gaToPhys = write(svshiInterfaceMock.getAssignmentGaToPhysId(), indent = 2)

    simulatorMock.started shouldBe true
    simulatorMock.config should (include(physStruct) and include(appBindings) and include(gaToPhys))

    svshiMock.running shouldBe true
    svshiMock.runningIpPort shouldEqual s"${session.getSimulatorAddr(docker = false)}:${session.SIMULATOR_KNX_PORT}"
  }

  "post simulation/start" should "first stop it if simulator is running and restart normally" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    simulatorMock.started = true
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulation/start",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200

    val physStruct = session.getOrComputePhysicalStructureJson(svshiInterfaceMock)
    val appBindings = svshiInterfaceMock.getAppLibraryBindings()
    val gaToPhys = write(svshiInterfaceMock.getAssignmentGaToPhysId(), indent = 2)

    simulatorMock.started shouldBe true
    simulatorMock.config should (include(physStruct) and include(appBindings) and include(gaToPhys))

    svshiMock.running shouldBe true
    svshiMock.runningIpPort shouldEqual s"${session.getSimulatorAddr(docker = false)}:${session.SIMULATOR_KNX_PORT}"
  }
  "post simulation/start" should "reply with 400 if svshi is running" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    svshiMock.running = true
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulation/start",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 400
  }

  "post simulation/stop" should "stop svshi and simulator" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    svshiMock.running = true
    simulatorMock.started = true
    val r =
      requests.post(
        f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulation/stop",
        check = false,
        data = write[List[String]](List(appName1)),
        readTimeout = requestsReadTimeout
      )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200

    svshiMock.running shouldBe false
    simulatorMock.started shouldBe false
  }

  "get /svshi/runStatus/" should "reply with the svshi run status" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    svshiMock.running = true
    val r = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/runStatus",
      check = false,
      readTimeout = requestsReadTimeout
    )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    read[Map[String, Boolean]](r.text()).get("status").get shouldBe true

    svshiMock.running = false
    val r2 = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/runStatus",
      check = false,
      readTimeout = requestsReadTimeout
    )
    expectedHeaders.foreach(p => r2.headers should containThePairOfHeaders(p))
    r2.statusCode shouldEqual 200
    read[Map[String, Boolean]](r2.text()).get("status").get shouldBe false
  }

  "get /simulator/runStatus/" should "reply with the svshi run status" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)
    val appName1 = "app_one"
    val appName2 = "app_two"
    val appName3 = "app_two"
    session.addApp(appName1, resMocksFolderPath / "app_one_generated")
    session.addApp(appName2, resMocksFolderPath / "app_one_generated")
    session.addApp(appName3, resMocksFolderPath / "app_one_generated")

    simulatorMock.started = true
    val r = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulator/runStatus",
      check = false,
      readTimeout = requestsReadTimeout
    )
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    read[Map[String, Boolean]](r.text()).get("status").get shouldBe true

    simulatorMock.started = false
    val r2 = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/simulator/runStatus",
      check = false,
      readTimeout = requestsReadTimeout
    )
    expectedHeaders.foreach(p => r2.headers should containThePairOfHeaders(p))
    r2.statusCode shouldEqual 200
    read[Map[String, Boolean]](r2.text()).get("status").get shouldBe false
  }

  "get svshi/logs/..." should "reply with the correct logs in a json" in {
    SessionManager.createNewSession()
    val session = SessionManager.getCurrentSession.get
    val file = resTestFolderPath / "DSLAB_proto.knxproj"
    session.setEtsFile(file)

    val rRun = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/logs/run",
      check = false,
      readTimeout = requestsReadTimeout
    )
    val logRunContentLines = List("run log line 1", "run log line 2")
    rRun.statusCode shouldEqual 200
    read[Map[String, List[String]]](rRun.text()).get("lines").get should contain theSameElementsInOrderAs logRunContentLines
    expectedHeaders.foreach(p => rRun.headers should containThePairOfHeaders(p))

    val rTelegrams = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/logs/receivedTelegrams",
      check = false,
      readTimeout = requestsReadTimeout
    )
    val logReceivedTelegramsContentLines = List("run receivedTelegrams line 1", "run receivedTelegrams line 2")
    rTelegrams.statusCode shouldEqual 200
    expectedHeaders.foreach(p => rTelegrams.headers should containThePairOfHeaders(p))
    read[Map[String, List[String]]](rTelegrams.text()).get("lines").get should contain theSameElementsInOrderAs logReceivedTelegramsContentLines

    val rExecution = requests.get(
      f"http://localhost:${Constants.WEB_SERVICE_DEFAULT_PORT}/svshi/logs/execution",
      check = false,
      readTimeout = requestsReadTimeout
    )
    val logExecutionContentLines = List("run execution line 1", "run execution line 2")
    rExecution.statusCode shouldEqual 200
    expectedHeaders.foreach(p => rExecution.headers should containThePairOfHeaders(p))
    read[Map[String, List[String]]](rExecution.text()).get("lines").get should contain theSameElementsInOrderAs logExecutionContentLines
  }

}
