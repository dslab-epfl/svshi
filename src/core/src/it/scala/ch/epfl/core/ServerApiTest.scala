package ch.epfl.core

import ch.epfl.core.CustomMatchers.{beAFile, beSimilarToLibrary, containThePairOfHeaders, existInFilesystem}
import ch.epfl.core.TestUtils.{compareFolders, defaultIgnoredFilesAndDir}
import ch.epfl.core.api.server.CoreApiServer
import ch.epfl.core.api.server.json.{BindingsResponse, ResponseBody}
import ch.epfl.core.deviceMapper.model.{DeviceMapping, StructureMapping, SupportedDeviceMapping, SupportedDeviceMappingNode}
import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical.BinarySensor
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.Constants._
import ch.epfl.core.utils.subprocess.SvshiSubProcessOs
import ch.epfl.core.utils.{Constants, FileUtils}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.MockitoSugar.{mock, reset, verify}
import org.mockito.captor.ArgCaptor
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.timeout
import org.scalatest.concurrent.{Signaler, TimeLimitedTests}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OneInstancePerTest}

import java.io.{ByteArrayOutputStream, FileInputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.language.{existentials, postfixOps}

class ServerApiTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with TimeLimitedTests with OneInstancePerTest {
  def timeLimit: Span = 30 seconds
  override val defaultTestSignaler: Signaler = (testThread: Thread) => testThread.stop()

  private val endToEndResPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "endToEnd"
  private val pipeline1Path = endToEndResPath / "pipeline1_app_one_valid"

  private val app1ProtoFileName = "test_app_one_proto.json"

  private lazy val app1ProtoStruct = AppInputJsonParser.parse(pipeline1Path / app1ProtoFileName)
  private lazy val app2ProtoStruct = AppInputJsonParser.parse(pipeline1Path / app1ProtoFileName)
  private lazy val existingAppsLibrary =
    ApplicationLibrary(List(Application("app1", Constants.APP_LIBRARY_FOLDER_PATH / "app1", app1ProtoStruct, Nil)), Constants.APP_LIBRARY_FOLDER_PATH)
  private lazy val newAppsLibrary = ApplicationLibrary(List(Application("app2", Constants.APP_LIBRARY_FOLDER_PATH / "app2", app2ProtoStruct, Nil)), Constants.GENERATED_FOLDER_PATH)
  private val existingPhysicalStructure = PhysicalStructure(Nil)

  private val backupGeneratedPath = SVSHI_SRC_FOLDER_PATH / "backup_generated_during_test"
  private val backupLibraryPath = SVSHI_SRC_FOLDER_PATH / "backup_library_during_test"
  private val backupInstalledAppsPath = SVSHI_SRC_FOLDER_PATH / "backup_installed_apps_during_test"
  private val backupAssignmentsPath = SVSHI_SRC_FOLDER_PATH / "backup_assignments"
  private val backupPythonRuntimeLogsPath = SVSHI_SRC_FOLDER_PATH / "backup_python_logs"

  private val runLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / "private_run_logs.log"

  private val coreResFolderPath: os.Path = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res"
  private val coreResApiServerFolderPath = coreResFolderPath / "apiServerTests"

  private val testPhysicalStructureJsonFileName = "test_physicalJson.json"
  private val testBindingsJsonFileName = "test_bindings_json_parser.json"

  private val simpleEtsProjFilePath = coreResFolderPath / "Simple.knxproj"
  private lazy val simpleEtsProjPhysStruct = EtsParser.parseEtsProjectFile(simpleEtsProjFilePath)
  private lazy val simpleEtsProjectZip = createInMemZip(simpleEtsProjFilePath)

  private val pythonScriptSleeps5Secs = "sleep5secsThenExit0.py"
  private val pythonScriptSleeps1Sec = "sleep1secThenExit0.py"

  private val fakeContentDirectoryPath = coreResApiServerFolderPath / "fakeContent"
  private val fakeContentDirectoryZipPath = coreResApiServerFolderPath / "fakeContent.zip"
  private val oldFakeContentDirectoryPath = coreResApiServerFolderPath / "fakeContentBeforeUpload"
  private val fakeContentAfterDirectoryPath = coreResApiServerFolderPath / "fakeContentAfterUpload"

  private val tempFolderPath: os.Path = coreResApiServerFolderPath / "tempTests"

  private val mockedSvshi = mock[SvshiTr]
  private var coreApiServer = CoreApiServer(mockedSvshi)

  private val expectedHeaders = Seq("Access-Control-Allow-Origin".toLowerCase -> "*")

  private val requestsReadTimeout = 60_000 // Milliseconds

  override def beforeAll(): Unit = {
    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInstalledAppsPath)) os.remove.all(backupInstalledAppsPath)
    if (os.exists(backupAssignmentsPath)) os.remove.all(backupAssignmentsPath)
    if (os.exists(backupPythonRuntimeLogsPath)) os.remove.all(backupPythonRuntimeLogsPath)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.copy(APP_LIBRARY_FOLDER_PATH, backupLibraryPath)
    if (os.exists(GENERATED_FOLDER_PATH)) os.copy(GENERATED_FOLDER_PATH, backupGeneratedPath)
    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.copy(INSTALLED_APPS_FOLDER_PATH, backupInstalledAppsPath)
    if (os.exists(ASSIGNMENTS_DIRECTORY_PATH)) os.copy(ASSIGNMENTS_DIRECTORY_PATH, backupAssignmentsPath)
    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) os.copy(PYTHON_RUNTIME_LOGS_FOLDER_PATH, backupPythonRuntimeLogsPath)
  }
  override def afterAll(): Unit = {
    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.remove.all(INSTALLED_APPS_FOLDER_PATH)
    if (os.exists(ASSIGNMENTS_DIRECTORY_PATH)) os.remove.all(ASSIGNMENTS_DIRECTORY_PATH)
    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) os.remove.all(PYTHON_RUNTIME_LOGS_FOLDER_PATH)

    if (os.exists(backupLibraryPath)) os.copy(backupLibraryPath, APP_LIBRARY_FOLDER_PATH)
    if (os.exists(backupGeneratedPath)) os.copy(backupGeneratedPath, GENERATED_FOLDER_PATH)
    if (os.exists(backupInstalledAppsPath)) os.copy(backupInstalledAppsPath, INSTALLED_APPS_FOLDER_PATH)
    if (os.exists(backupAssignmentsPath)) os.copy(backupAssignmentsPath, ASSIGNMENTS_DIRECTORY_PATH)
    if (os.exists(backupPythonRuntimeLogsPath)) os.copy(backupPythonRuntimeLogsPath, PYTHON_RUNTIME_LOGS_FOLDER_PATH)

    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInstalledAppsPath)) os.remove.all(backupInstalledAppsPath)
    if (os.exists(backupAssignmentsPath)) os.remove.all(backupAssignmentsPath)
    if (os.exists(backupPythonRuntimeLogsPath)) os.remove.all(backupPythonRuntimeLogsPath)

    if (os.exists(tempFolderPath)) os.remove.all(tempFolderPath)

    if (os.exists(Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH)) os.remove(Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH)
  }

  override def beforeEach(): Unit = {
    if (os.exists(Constants.PRIVATE_INPUT_FOLDER_PATH)) os.remove.all(Constants.PRIVATE_INPUT_FOLDER_PATH)
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.remove.all(INSTALLED_APPS_FOLDER_PATH)
    os.makeDir.all(INSTALLED_APPS_FOLDER_PATH)

    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) os.remove.all(PYTHON_RUNTIME_LOGS_FOLDER_PATH)
    os.makeDir.all(PYTHON_RUNTIME_LOGS_FOLDER_PATH)

    if (os.exists(tempFolderPath)) os.remove.all(tempFolderPath)
    os.makeDir.all(tempFolderPath)

    if (os.exists(Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH)) os.remove(Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH)

    reset(mockedSvshi)
    coreApiServer = CoreApiServer(mockedSvshi)
    coreApiServer.start()

  }

  override def afterEach(): Unit = {
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    os.makeDir.all(GENERATED_FOLDER_PATH)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    os.makeDir.all(APP_LIBRARY_FOLDER_PATH)

    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.remove.all(INSTALLED_APPS_FOLDER_PATH)
    os.makeDir.all(INSTALLED_APPS_FOLDER_PATH)

    if (os.exists(Constants.PRIVATE_INPUT_FOLDER_PATH)) os.remove.all(Constants.PRIVATE_INPUT_FOLDER_PATH)

    coreApiServer.stop()
  }

  "getVersion endpoint" should "respond code 200 and the correct version as JSON" in {
    mockedSvshi.getVersion(*) shouldAnswer ((success: String => Unit) => {
      success(f"svshi v0.0.0")
      0
    })

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/version", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output.length shouldEqual 1
    responseBody.output.head.contains("svshi v") shouldEqual true
  }

  "getVersion endpoint" should "respond code 200 and the correct version as JSON with custom port" in {
    mockedSvshi.getVersion(*) shouldAnswer ((success: String => Unit) => {
      success(f"svshi v0.0.0")
      0
    })
    coreApiServer.stop()
    coreApiServer = CoreApiServer(svshiSystem = mockedSvshi, port = 6789)
    coreApiServer.start()

    val r = requests.get(f"http://localhost:6789/version", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output.length shouldEqual 1
    responseBody.output.head.contains("svshi v") shouldEqual true
  }

  "getVersion endpoint" should "respond code 200 and the correct version as JSON with custom host and port" in {
    mockedSvshi.getVersion(*) shouldAnswer ((success: String => Unit) => {
      success(f"svshi v0.0.0")
      0
    })
    coreApiServer.stop()
    coreApiServer = CoreApiServer(svshiSystem = mockedSvshi, host = "0.0.0.0", port = 4243)
    coreApiServer.start()

    val r = requests.get(f"http://0.0.0.0:4243/version", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output.length shouldEqual 1
    responseBody.output.head.contains("svshi v") shouldEqual true
  }

  "generateApp endpoint" should "call the Svshi.generateApp function with the correct arguments" in {
    // Prepare everything for the test

    val protoFileName = "test_app_one_proto.json"
    val protoFilePath = pipeline1Path / protoFileName
    val protoJsonString = os.read(protoFilePath)

    val appName = "test_app_one"

    mockedSvshi.generateApp(*, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: Option[String], success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success(f"The app '$appName' has been successfully created!")
        0
      }
    )

    val captorAppName = ArgCaptor[Option[String]]
    val captorJsonFilePath = ArgCaptor[Option[String]]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateApp/$appName", data = protoJsonString, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())

    // Checks
    responseBody.output.mkString("\n") should (include(s"The app '$appName' has been successfully created!"))

    verify(mockedSvshi).generateApp(captorAppName, captorJsonFilePath)(captorSuccess, captorInfo, captorWarning, captorError)

    val createdFile = Constants.PRIVATE_INPUT_FOLDER_PATH / Constants.APP_PROTO_STRUCT_FILE_NAME
    captorAppName hasCaptured Some(appName)
    captorJsonFilePath hasCaptured Some(createdFile.toString())

    // Check the prototypical structure file has been created by the API server to be used to generate the app
    os.exists(createdFile) shouldBe true
    AppInputJsonParser.parse(createdFile) shouldEqual AppInputJsonParser.parse(protoFilePath)
  }

  "removeApp endpoint" should "call the remove method with the passed appName and replies with output" in {

    mockedSvshi.removeApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Boolean, appName: Option[String], _: ApplicationLibrary, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success(f"${appName.get} was removed!")
        0
      }
    )

    val captorFlag = ArgCaptor[Boolean]
    val captorAppName = ArgCaptor[Option[String]]
    val captorAppLibrary = ArgCaptor[ApplicationLibrary]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val appName = "appTestRemove42"
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/removeApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())

    verify(mockedSvshi).removeApps(captorFlag, captorAppName, captorAppLibrary)(captorSuccess, captorInfo, captorWarning, captorError)

    captorFlag hasCaptured false
    captorAppName hasCaptured Some(appName)
    captorAppLibrary hasCaptured ApplicationLibrary(Nil, Constants.APP_LIBRARY_FOLDER_PATH)
    responseBody.status shouldEqual true
    val expectedOutput = f"$appName was removed!"
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "removeAllApps endpoint" should "call the remove method with no names and allFlag = true and replies with output" in {

    mockedSvshi.removeApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Boolean, _: Option[String], _: ApplicationLibrary, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("All apps were removed!")
        0
      }
    )

    val captorFlag = ArgCaptor[Boolean]
    val captorAppName = ArgCaptor[Option[String]]
    val captorAppLibrary = ArgCaptor[ApplicationLibrary]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/removeAllApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())

    verify(mockedSvshi).removeApps(captorFlag, captorAppName, captorAppLibrary)(captorSuccess, captorInfo, captorWarning, captorError)

    captorFlag hasCaptured true
    captorAppName hasCaptured None
    captorAppLibrary hasCaptured ApplicationLibrary(Nil, Constants.APP_LIBRARY_FOLDER_PATH)
    responseBody.status shouldEqual true
    val expectedOutput = "All apps were removed!"
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "run endpoint" should "log the messages with correct prefixes of run command in the private log file when run write directly messages" in {

    if (os.exists(runLogFilePath)) os.remove(runLogFilePath)
    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("this is a success message sent by run command")
        info("this is an info message sent by run command")
        warning("this is a warning message sent by run command")
        error("this is an error message sent by run command")
        new SvshiRunResult(None, 0)
      }
    )
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(7 seconds)) {
      runLogFilePath should existInFilesystem
      runLogFilePath should beAFile
      val expectedContent =
        """
          |this is a success message sent by run command
          |INFO: this is an info message sent by run command
          |WARNING: this is a warning message sent by run command
          |ERROR: this is an error message sent by run command
          |""".stripMargin.trim
      os.read(runLogFilePath).trim shouldEqual expectedContent
    }
  }

  "run endpoint" should "log the messages with correct prefixes of run command in the private log file when run write after some delay" in {

    val logFile = runLogFilePath
    if (os.exists(logFile)) os.remove(logFile)
    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("this is a success message sent by run command")
        Thread.sleep(100)
        info("this is an info message sent by run command")
        Thread.sleep(100)
        warning("this is a warning message sent by run command")
        Thread.sleep(1000)
        error("this is an error message sent by run command")
        new SvshiRunResult(None, 0)
      }
    )
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    eventually(timeout(7 seconds)) { logFile should existInFilesystem }
    eventually(timeout(7 seconds)) { logFile should beAFile }
    val expectedContent =
      """
        |this is a success message sent by run command
        |INFO: this is an info message sent by run command
        |WARNING: this is a warning message sent by run command
        |ERROR: this is an error message sent by run command
        |""".stripMargin.trim
    eventually(timeout(7 seconds)) { os.read(logFile).trim shouldEqual expectedContent }
  }

  "run endpoint" should "send a message saying the run command was initiated" in {

    val logFile = runLogFilePath
    if (os.exists(logFile)) os.remove(logFile)
    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        new SvshiRunResult(None, 0)
      }
    )
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text)
    responseBody.status shouldBe true
    responseBody.output.mkString("\n").trim shouldEqual "SVSHI run was initiated!"
  }

  "runStatus endpoint" should "reply with a true status if run is running" in {

    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        val p = os.proc("python3", pythonScriptSleeps1Sec).spawn(coreResFolderPath)
        val svshiSubProcess = new SvshiSubProcessOs(p)
        new SvshiRunResult(Some(svshiSubProcess), 0)
      }
    )
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
  }

  "runStatus endpoint" should "reply with a false status if run stops running by itself" in {

    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        val p = os.proc("python3", pythonScriptSleeps1Sec).spawn(coreResFolderPath)
        val svshiSubProcess = new SvshiSubProcessOs(p)
        new SvshiRunResult(Some(svshiSubProcess), 0)
      }
    )
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    eventually(timeout(3 seconds)) {
      val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
      expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
      r.statusCode shouldEqual 200
      val responseBody = ResponseBody.from(r.text())
      responseBody.status shouldBe false
    }
  }

  "runStatus endpoint" should "reply with a false status if run is not running" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
  }

  "logs/run endpoint" should "reply with the content of the log file when run is running" in {

    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("this is a success message sent by run command")
        info("this is an info message sent by run command")
        warning("this is a warning message sent by run command")
        error("this is an error message sent by run command")
        val p = os.proc("python3", pythonScriptSleeps5Secs).spawn(coreResFolderPath)
        val svshiSubProcess = new SvshiSubProcessOs(p)
        new SvshiRunResult(Some(svshiSubProcess), 0)
      }
    )
    val expectedContent =
      """
        |this is a success message sent by run command
        |INFO: this is an info message sent by run command
        |WARNING: this is a warning message sent by run command
        |ERROR: this is an error message sent by run command
        |""".stripMargin.trim
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)

    eventually(timeout(1 seconds)) {
      val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/run", check = false, readTimeout = requestsReadTimeout)
      expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
      r.statusCode shouldEqual 200
      val responseBody = ResponseBody.from(r.text())
      responseBody.status shouldBe true
      responseBody.output.mkString("\n").trim shouldEqual expectedContent
    }
  }

  "logs/run endpoint" should "reply with false status if the run command was never called" in {
    val localRunLogFileName = "private_run_logs.log"
    val localRunLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / localRunLogFileName
    os.remove(localRunLogFilePath)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/run", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
  }

  "logs/run endpoint" should "reply with true status and the last log if the run command is not running" in {
    val localRunLogFileName = "private_run_logs.log"
    val localRunLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / localRunLogFileName
    val log = "test log"
    FileUtils.writeToFileOverwrite(localRunLogFilePath, log.getBytes)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/run", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List(log)
  }

  "logs/receivedTelegrams" should "reply with the latest telegrams_received.log file content when it exists" in {
    val f1 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__12_39_45.675001"
    val f2 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__13_46_18.978512"
    val f3 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__17_24_36.126687"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.makeDir.all(f3)

    FileUtils.writeToFileOverwrite(f1 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 1".getBytes)
    FileUtils.writeToFileOverwrite(f2 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 2".getBytes)
    FileUtils.writeToFileOverwrite(f3 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 3 1\n".getBytes)
    FileUtils.writeToFileAppend(f3 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 3 2".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/receivedTelegrams", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List("received telegram 3 1", "received telegram 3 2")
  }

  "logs/receivedTelegrams" should "reply with empty list and false when the latest folder has not received telegrams file" in {
    val f1 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__12_39_45.675001"
    val f2 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__13_46_18.978512"
    val f3 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__17_24_36.126687"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.makeDir.all(f3)

    FileUtils.writeToFileOverwrite(f1 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 1".getBytes)
    FileUtils.writeToFileOverwrite(f2 / PYTHON_RUNTIME_LOGS_RECEIVED_TELEGRAMS_LOG_FILE_NAME, "received telegram 2".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/receivedTelegrams", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
    responseBody.output shouldEqual Nil
  }

  "logs/receivedTelegrams" should "reply with empty list and false when there is no logs folder" in {

    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) os.remove.all(PYTHON_RUNTIME_LOGS_FOLDER_PATH)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/receivedTelegrams", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
    responseBody.output shouldEqual Nil
  }

  "logs/execution" should "reply with the latest telegrams_received.log file content when it exists" in {
    val f1 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__12_39_45.675001"
    val f2 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__13_46_18.978512"
    val f3 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__17_24_36.126687"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.makeDir.all(f3)

    FileUtils.writeToFileOverwrite(f1 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 1".getBytes)
    FileUtils.writeToFileOverwrite(f2 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 2".getBytes)
    FileUtils.writeToFileOverwrite(f3 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 3 1\n".getBytes)
    FileUtils.writeToFileAppend(f3 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 3 2".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/execution", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List("execution 3 1", "execution 3 2")
  }

  "logs/execution" should "reply with empty list and false when the latest folder has not execution file" in {
    val f1 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__12_39_45.675001"
    val f2 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__13_46_18.978512"
    val f3 = PYTHON_RUNTIME_LOGS_FOLDER_PATH / "2022-05-02__17_24_36.126687"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.makeDir.all(f3)

    FileUtils.writeToFileOverwrite(f1 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 1".getBytes)
    FileUtils.writeToFileOverwrite(f2 / PYTHON_RUNTIME_LOGS_EXECUTION_LOG_FILE_NAME, "execution 2".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/execution", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
    responseBody.output shouldEqual Nil
  }

  "logs/execution" should "reply with empty list and false when there is no logs folder" in {

    if (os.exists(PYTHON_RUNTIME_LOGS_FOLDER_PATH)) os.remove.all(PYTHON_RUNTIME_LOGS_FOLDER_PATH)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/execution", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
    responseBody.output shouldEqual Nil
  }

  "logs/physicalState" should "reply with empty list and false when no physicalState log file exist" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/physicalState", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "logs/physicalState" should "reply with the content of the physical state log file when the physicalState log file exists" in {
    val f1 = Constants.PYTHON_RUNTIME_LOGS_PHYSICAL_STATE_LOG_FILE_PATH

    val json = """|{
                  |  "GA_0_0_1": true,
                  |  "GA_0_0_2": 2.0
                  |}
                  |""".stripMargin
    FileUtils.writeToFileOverwrite(
      f1,
      json.getBytes
    )

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/physicalState", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    r.text() shouldEqual json
  }

  "stopRun endpoint" should "stop the run thread when called and reply with the message" in {

    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        val p = os.proc("python3", pythonScriptSleeps5Secs).spawn(coreResFolderPath)
        val svshiSubProcess = new SvshiSubProcessOs(p)
        new SvshiRunResult(Some(svshiSubProcess), 0)
      }
    )
    val r1 = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true

    val r2 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r2.headers should containThePairOfHeaders(p))
    r2.statusCode shouldEqual 200
    val responseBody2 = ResponseBody.from(r2.text())
    responseBody2.status shouldBe true

    val r3 = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/stopRun", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r3.headers should containThePairOfHeaders(p))
    r3.statusCode shouldEqual 200
    val responseBody3 = ResponseBody.from(r3.text())
    responseBody3.status shouldBe true
    responseBody3.output.mkString("\n") shouldEqual "SVSHI run was stopped!"

    eventually(timeout(1 seconds)) {
      val r4 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
      expectedHeaders.foreach(p => r4.headers should containThePairOfHeaders(p))
      r4.statusCode shouldEqual 200
      val responseBody4 = ResponseBody.from(r4.text())
      responseBody4.status shouldBe false
    }
  }

  "newApps endpoint" should "reply with the list of folders in generated" in {
    val app1Name = "app1"
    val app2Name = "app2"
    os.makeDir(Constants.GENERATED_FOLDER_PATH / app1Name)
    os.makeDir(Constants.GENERATED_FOLDER_PATH / app2Name)

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List(app1Name, app2Name)
  }

  "newApps endpoint" should "ignore files" in {
    val app1Name = "app1"
    val app2Name = "app2"
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / app1Name)
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / app2Name)

    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "file1", "test".getBytes)

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List(app1Name, app2Name)
  }

  "newApps endpoint" should "reply with the list of folders in generated when empty" in {

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual Nil
  }

  "compile endpoint" should "call the compile function with the correct physicalStructures with .json file" in {

    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("Applications were installed!")
        0
      }
    )
    val knxProjFileZip = createInMemZip(simpleEtsProjFilePath)

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).compileApps(captorExistingApps, captorNewApps, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedPhysStruct = simpleEtsProjPhysStruct
    captorPhysStruct hasCaptured expectedPhysStruct

    val expectedOutput = "Applications were installed!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "compile endpoint" should "call the generateBindings function with the correct physicalStructures with .json file" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("Applications were installed!")
        0
      }
    )
    val existingPhysStructJsonFile = existingAppsLibrary.path / "physical_structure.json"
    val expectedExistingPhysStruct = simpleEtsProjPhysStruct
    PhysicalStructureJsonParser.writeToFile(existingPhysStructJsonFile, expectedExistingPhysStruct)
    PhysicalStructureJsonParser.writeToFile(tempFolderPath / "input_json_struct.json", expectedExistingPhysStruct)
    val knxProjFileZip = createInMemZip(tempFolderPath / "input_json_struct.json")

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).compileApps(captorExistingApps, captorNewApps, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedPhysStruct = simpleEtsProjPhysStruct
    captorPhysStruct hasCaptured expectedPhysStruct

    val expectedOutput = "Applications were installed!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "compile endpoint" should "fail with a file which is not a json nor a knxproj file" in {

    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("Applications were installed!")
        0
      }
    )
    val wrongFilePath = tempFolderPath / "input_json_struct.png"
    os.write(wrongFilePath, "test".getBytes())
    val newKnxProjFileZip = createInMemZip(wrongFilePath)

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 400
    r.text() shouldEqual "The file for the physical structure must be either a json or a knxproj file!"
  }

  "compile endpoint" should "call the compile function with the correct Application libraries" in {

    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        success("Applications were installed!")
        0
      }
    )

    // Create app libraries:
    val app1Path = Constants.APP_LIBRARY_FOLDER_PATH / "app1"
    val app2Path = Constants.GENERATED_FOLDER_PATH / "app2"
    os.makeDir.all(app1Path)
    os.makeDir.all(app2Path)
    os.copy(pipeline1Path / app1ProtoFileName, app1Path / Constants.APP_PROTO_STRUCT_FILE_NAME)
    os.copy(pipeline1Path / app1ProtoFileName, app2Path / Constants.APP_PROTO_STRUCT_FILE_NAME)

    val knxProjFile = simpleEtsProjFilePath
    val knxProjFileZip = createInMemZip(knxProjFile)

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).compileApps(captorExistingApps, captorNewApps, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorNewApps.value should beSimilarToLibrary(newAppsLibrary)
    captorExistingApps.value should beSimilarToLibrary(existingAppsLibrary)

    val expectedOutput = "Applications were installed!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "updateApp endpoint" should "reply with a 404 when app is not installed" in {

    mockedSvshi.updateApp(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, appname: String, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success(f"$appname was updated!")
        0
      }
    )

    val appName = "appNameBla42"

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "updateApp endpoint" should "call the updateApp function with the correct appName when app is installed" in {

    mockedSvshi.updateApp(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, appname: String, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success(f"$appname was updated!")
        0
      }
    )
    // Create app libraries:
    val app1Path = Constants.APP_LIBRARY_FOLDER_PATH / "app1"
    os.makeDir.all(app1Path)
    os.copy(pipeline1Path / app1ProtoFileName, app1Path / Constants.APP_PROTO_STRUCT_FILE_NAME)

    val appName = "app1"
    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorAppName = ArgCaptor[String]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    verify(mockedSvshi).updateApp(captorExistingApps, captorNewApps, captorAppName, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorAppName hasCaptured appName

    val expectedOutput = f"$appName was updated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "updateApp endpoint" should "call the compile function with the correct Application libraries" in {

    mockedSvshi.updateApp(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, appname: String, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success(f"$appname was updated!")
        0
      }
    )
    // Create app libraries:
    val app1Path = Constants.APP_LIBRARY_FOLDER_PATH / "app1"
    val app2Path = Constants.GENERATED_FOLDER_PATH / "app2"
    os.makeDir.all(app1Path)
    os.makeDir.all(app2Path)
    os.copy(pipeline1Path / app1ProtoFileName, app1Path / Constants.APP_PROTO_STRUCT_FILE_NAME)
    os.copy(pipeline1Path / app1ProtoFileName, app2Path / Constants.APP_PROTO_STRUCT_FILE_NAME)

    val appName = "app1"

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorAppName = ArgCaptor[String]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    verify(mockedSvshi).updateApp(captorExistingApps, captorNewApps, captorAppName, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorNewApps.value should beSimilarToLibrary(newAppsLibrary)
    captorExistingApps.value should beSimilarToLibrary(existingAppsLibrary)

    val expectedOutput = f"$appName was updated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "updateApp endpoint" should "call the compile function with the correct PhysicalStructure" in {

    mockedSvshi.updateApp(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, appname: String, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success(f"$appname was updated!")
        0
      }
    )
    // Create app libraries:
    val app1Path = Constants.APP_LIBRARY_FOLDER_PATH / "app1"
    val app2Path = Constants.GENERATED_FOLDER_PATH / "app2"
    os.makeDir.all(app1Path)
    os.makeDir.all(app2Path)
    os.copy(pipeline1Path / app1ProtoFileName, app1Path / Constants.APP_PROTO_STRUCT_FILE_NAME)
    os.copy(pipeline1Path / app1ProtoFileName, app2Path / Constants.APP_PROTO_STRUCT_FILE_NAME)

    val existingPhysStructJsonFile = existingAppsLibrary.path / "physical_structure.json"
    val expectedExistingPhysStruct = simpleEtsProjPhysStruct
    PhysicalStructureJsonParser.writeToFile(existingPhysStructJsonFile, expectedExistingPhysStruct)

    val appName = "app1"

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorAppName = ArgCaptor[String]
    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    verify(mockedSvshi).updateApp(captorExistingApps, captorNewApps, captorAppName, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorPhysStruct.value shouldEqual expectedExistingPhysStruct

    val expectedOutput = f"$appName was updated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "generateBindings endpoint" should "call the generateBindings function with the correct physicalStructures with .knxproj file" in {

    mockedSvshi.generateBindings(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success("Bindings were generated!")
        0
      }
    )
    val existingPhysStructJsonFile = existingAppsLibrary.path / "physical_structure.json"
    val expectedExistingPhysStruct = simpleEtsProjPhysStruct
    PhysicalStructureJsonParser.writeToFile(existingPhysStructJsonFile, expectedExistingPhysStruct)
    val newKnxProjFileZip = createInMemZip(simpleEtsProjFilePath)

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorNewPhysStruct = ArgCaptor[PhysicalStructure]
    val captorExistingPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generateBindings(captorExistingApps, captorNewApps, captorExistingPhysStruct, captorNewPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedNewPhysStruct = simpleEtsProjPhysStruct

    val expectedOutput = "Bindings were generated!"
    val responseBody = ResponseBody.from(r.text())

    captorNewPhysStruct hasCaptured expectedNewPhysStruct
    captorExistingPhysStruct hasCaptured expectedExistingPhysStruct
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "generateBindings endpoint" should "call the generateBindings function with the correct physicalStructures with .json file" in {

    mockedSvshi.generateBindings(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success("Bindings were generated!")
        0
      }
    )
    val existingPhysStructJsonFile = existingAppsLibrary.path / "physical_structure.json"
    val expectedExistingPhysStruct = simpleEtsProjPhysStruct
    PhysicalStructureJsonParser.writeToFile(existingPhysStructJsonFile, expectedExistingPhysStruct)
    PhysicalStructureJsonParser.writeToFile(tempFolderPath / "input_json_struct.json", expectedExistingPhysStruct)
    val newKnxProjFileZip = createInMemZip(tempFolderPath / "input_json_struct.json")

    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorNewPhysStruct = ArgCaptor[PhysicalStructure]
    val captorExistingPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generateBindings(captorExistingApps, captorNewApps, captorExistingPhysStruct, captorNewPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedNewPhysStruct = simpleEtsProjPhysStruct

    val expectedOutput = "Bindings were generated!"
    val responseBody = ResponseBody.from(r.text())

    captorNewPhysStruct hasCaptured expectedNewPhysStruct
    captorExistingPhysStruct hasCaptured expectedExistingPhysStruct
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "generateBindings endpoint" should "fail with a file which is not a json nor a knxproj file" in {

    mockedSvshi.generateBindings(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success("Bindings were generated!")
        0
      }
    )
    val wrongFilePath = tempFolderPath / "input_json_struct.png"
    os.write(wrongFilePath, "test".getBytes())
    val newKnxProjFileZip = createInMemZip(wrongFilePath)

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 400
    r.text() shouldEqual "The file for the physical structure must be either a json or a knxproj file!"
  }

  "generateBindings endpoint" should "call the generateBindings function with the correct Application libraries" in {

    mockedSvshi.generateBindings(*, *, *, *)(*, *, *, *) shouldAnswer (
      (
        _: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit,
        error: String => Unit
      ) => {
        success("Bindings were generated!")
        0
      }
    )

    // Create app libraries:
    val app1Path = Constants.APP_LIBRARY_FOLDER_PATH / "app1"
    val app2Path = Constants.GENERATED_FOLDER_PATH / "app2"
    os.makeDir.all(app1Path)
    os.makeDir.all(app2Path)
    os.copy(pipeline1Path / app1ProtoFileName, app1Path / Constants.APP_PROTO_STRUCT_FILE_NAME)
    os.copy(pipeline1Path / app1ProtoFileName, app2Path / Constants.APP_PROTO_STRUCT_FILE_NAME)

    val newKnxProjFile = simpleEtsProjFilePath
    val newKnxProjFileZip = createInMemZip(newKnxProjFile)
    val captorExistingApps = ArgCaptor[ApplicationLibrary]
    val captorNewApps = ArgCaptor[ApplicationLibrary]
    val captorNewPhysStruct = ArgCaptor[PhysicalStructure]
    val captorExistingPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generateBindings(captorExistingApps, captorNewApps, captorExistingPhysStruct, captorNewPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorNewApps.value should beSimilarToLibrary(newAppsLibrary)
    captorExistingApps.value should beSimilarToLibrary(existingAppsLibrary)

    val expectedOutput = "Bindings were generated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "bindings endpoint" should "reply with a 404 when no bindings and physicalStructure files are in generated" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "bindings endpoint" should "reply with a 404 when no bindings file is in generated but one physical_structure is" in {
    os.copy(coreResFolderPath / testPhysicalStructureJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)

    Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME should existInFilesystem
    Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME should beAFile
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "bindings endpoint" should "reply with a 404 when no physical_structure file is in generated but one bindings is" in {
    os.copy(coreResFolderPath / testBindingsJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME)

    Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME should existInFilesystem
    Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME should beAFile
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "bindings endpoint" should "reply with a 200 and  correct physical structure and bindings when both file are in the generated folder" in {
    os.copy(coreResFolderPath / testPhysicalStructureJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)
    os.copy(coreResFolderPath / testBindingsJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME)

    coreResFolderPath / testPhysicalStructureJsonFileName should existInFilesystem
    coreResFolderPath / testPhysicalStructureJsonFileName should beAFile
    coreResFolderPath / testBindingsJsonFileName should existInFilesystem
    coreResFolderPath / testBindingsJsonFileName should beAFile

    val expectedPhysicalStruct = PhysicalStructureJsonParser.parseJson(os.read(coreResFolderPath / testPhysicalStructureJsonFileName))
    val expectedBindings = BindingsJsonParser.parse(coreResFolderPath / testBindingsJsonFileName)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val bindingsResponse = BindingsResponse.from(r.text())
    bindingsResponse.bindings shouldEqual expectedBindings
    bindingsResponse.physicalStructure shouldEqual expectedPhysicalStruct
  }

  "generated post endpoint" should "move all files to generated folder when it is empty before the operation and send list in body" in {
    val data = os.read.bytes(fakeContentDirectoryZipPath)
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", data = data)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
    responseBody.output.exists(s => s.contains("dir1")) shouldBe true
    responseBody.output.exists(s => s.contains("dir1/subdir1")) shouldBe true
    responseBody.output.exists(s => s.contains("dir1/subdir1/file11.txt")) shouldBe true
    responseBody.output.exists(s => s.contains("dir1/file1.txt")) shouldBe true
    responseBody.output.exists(s => s.contains("file0.txt")) shouldBe true

    compareFolders(folder1 = fakeContentDirectoryPath, folder2 = Constants.GENERATED_FOLDER_PATH, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
  }

  "generated post endpoint" should "move all files to generated folder when some files were there before the operation and preserve unmodified ones" in {
    oldFakeContentDirectoryPath / "file2.txt" should existInFilesystem
    oldFakeContentDirectoryPath should existInFilesystem
    os.copy.over(oldFakeContentDirectoryPath, Constants.GENERATED_FOLDER_PATH)

    val data = os.read.bytes(fakeContentDirectoryZipPath)
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", data = data)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    compareFolders(folder1 = fakeContentAfterDirectoryPath, folder2 = Constants.GENERATED_FOLDER_PATH, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
  }

  "deleteAllGenerated endpoint" should "remove everything that is contained in the generated folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deleteAllGenerated")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output should contain("The generated folder was successfully emptied!")

    os.list(Constants.GENERATED_FOLDER_PATH).toList shouldEqual Nil
  }

  "deleteGenerated endpoint" should "remove only the requested file in the generated folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile", "test".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deleteGenerated/aFile")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output should contain("'aFile' was successfully removed!")

    os.list(Constants.GENERATED_FOLDER_PATH).toList shouldEqual List(Constants.GENERATED_FOLDER_PATH / "aDir")
    os.list(Constants.GENERATED_FOLDER_PATH / "aDir").toList shouldEqual List(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile")
  }

  "deleteGenerated endpoint" should "remove only the requested directory in the generated folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile", "test".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deleteGenerated/aDir")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output should contain("'aDir' was successfully removed!")

    os.list(Constants.GENERATED_FOLDER_PATH).toList shouldEqual List(Constants.GENERATED_FOLDER_PATH / "aFile")
  }

  "deleteGenerated endpoint" should "reply with a success message if the filename does not exist in the generated folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile", "test".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deleteGenerated/doesNotExist")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output should contain("'doesNotExist' was successfully removed!")

    os.list(Constants.GENERATED_FOLDER_PATH).toList should contain theSameElementsAs List(Constants.GENERATED_FOLDER_PATH / "aFile", Constants.GENERATED_FOLDER_PATH / "aDir")
    os.list(Constants.GENERATED_FOLDER_PATH / "aDir").toList shouldEqual List(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile")
  }

  "installedApp get appName endpoint" should "reply with a 404 if the requested app is not installed" in {
    val appName = "notInstalled"
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/installedApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "installedApp get appName endpoint" should "reply with a zip of the requested app in the body if it is installed" in {
    // Prepare
    val appName = "test_app_one"
    val app1Path = pipeline1Path / "expected_library" / appName
    os.copy.into(app1Path, Constants.INSTALLED_APPS_FOLDER_PATH)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/installedApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath) match {
      case Some(p) => {
        p / appName should existInFilesystem
        compareFolders(app1Path, p / appName, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "allInstalledApps endpoint" should "reply with a 404 if no app is installed" in {
    if (os.exists(Constants.INSTALLED_APPS_FOLDER_PATH)) os.remove.all(Constants.INSTALLED_APPS_FOLDER_PATH)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/allInstalledApps/", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "allInstalledApps endpoint" should "reply with a zip of the installedApps folder in the body if one is installed, with bindings and physical structure" in {
    // Prepare
    val appName = "test_app_one"
    val app1Path = pipeline1Path / "expected_library" / appName
    os.copy.into(app1Path, Constants.INSTALLED_APPS_FOLDER_PATH)
    FileUtils.writeToFileOverwrite(Constants.INSTALLED_APPS_FOLDER_PATH / "app_bindings.json", "bindings".getBytes)
    FileUtils.writeToFileOverwrite(Constants.INSTALLED_APPS_FOLDER_PATH / "physical_structure.json", "physical structure".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/allInstalledApps/", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath) match {
      case Some(p) => {
        val basePath = p / "installedApps"
        basePath should existInFilesystem
        basePath shouldNot beAFile
        basePath / appName should existInFilesystem
        compareFolders(app1Path, basePath / appName, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
        basePath / "app_bindings.json" should existInFilesystem
        os.read(basePath / "app_bindings.json") shouldEqual "bindings"
        basePath / "physical_structure.json" should existInFilesystem
        os.read(basePath / "physical_structure.json") shouldEqual "physical structure"
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "generated get endpoint" should "reply with the content of the generated folder in a zip when empty" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        os.list(p) shouldEqual Nil
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "generated get endpoint" should "reply with the content of the generated folder in a zip when containing files and dirs" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "dir1")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "dir1" / "file11.txt", "test file11".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "file0.txt", "test file0".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        compareFolders(p, Constants.GENERATED_FOLDER_PATH, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "generated get endpoint with name" should "reply with an empty zip when the requested name is not in the folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aDir" / "aFile", "test".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated/doesNotExist", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        os.list(p) shouldEqual Nil
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "generated get endpoint with name" should "reply with a zip containing the requested file when it exists" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "dir1")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "dir1" / "file11.txt", "test file11".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "file0.txt", "test file0".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated/file0.txt", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        os.list(p) shouldEqual List(p / "file0.txt")
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "generated get endpoint with name" should "reply with a zip containing the requested directory when it exists" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "dir1")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "dir1" / "file11.txt", "test file11".getBytes)
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "file0.txt", "test file0".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated/dir1", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        os.list(p) shouldEqual List(p / "dir1")
        compareFolders(p / "dir1", Constants.GENERATED_FOLDER_PATH / "dir1", ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  "listApps endpoint" should "return the correct list with the installed apps in the outputs" in {

    mockedSvshi.listApps(*) shouldAnswer ((_: ApplicationLibrary) => {
      List("app1", "app2")
    })

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/listApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())

    responseBody.status shouldEqual true
    val expectedOutput = "app1\napp2"
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "listApps endpoint" should "return an empty list when no apps are installed" in {

    mockedSvshi.listApps(*) shouldAnswer ((_: ApplicationLibrary) => {
      Nil
    })

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/listApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())

    responseBody.status shouldEqual true
    responseBody.output shouldEqual Nil
  }

  "availableProtoDevices endpoint" should "return the available devices for new apps" in {

    val expectedDevices = List("device1", "device2")
    mockedSvshi.getAvailableProtoDevices() shouldReturn expectedDevices

    mockedSvshi.getAvailableProtoDevices() shouldEqual expectedDevices
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/availableProtoDevices")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    responseBody.output should contain theSameElementsAs expectedDevices
  }

  "availableDpts endpoint" should "return the available dpts" in {

    val expectedDpts = List("DPT-1", "DPT-2")
    mockedSvshi.getAvailableDpts() shouldReturn expectedDpts

    mockedSvshi.getAvailableDpts() shouldEqual expectedDpts
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/availableDpts")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    responseBody.output should contain theSameElementsAs expectedDpts
  }

  "assignments endpoint" should "reply with a 404 when the assignments does not exist" in {
    if (os.exists(Constants.ASSIGNMENTS_DIRECTORY_PATH)) os.remove.all(Constants.ASSIGNMENTS_DIRECTORY_PATH)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/assignments", check = false)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }
  "assignments endpoint" should "reply with a 404 when the assignments is empty" in {
    if (os.exists(Constants.ASSIGNMENTS_DIRECTORY_PATH)) os.remove.all(Constants.ASSIGNMENTS_DIRECTORY_PATH)
    os.makeDir(Constants.ASSIGNMENTS_DIRECTORY_PATH)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/assignments", check = false)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }
  "assignments endpoint" should "reply with a a zip of the assignments when it exists and assignments file are in it" in {
    if (os.exists(Constants.ASSIGNMENTS_DIRECTORY_PATH)) os.remove.all(Constants.ASSIGNMENTS_DIRECTORY_PATH)
    os.makeDir(Constants.ASSIGNMENTS_DIRECTORY_PATH)
    FileUtils.writeToFileOverwrite(Constants.ASSIGNMENTS_DIRECTORY_PATH / "assignments.txt", "this is an assignment file txt".getBytes)
    FileUtils.writeToFileOverwrite(Constants.ASSIGNMENTS_DIRECTORY_PATH / "assignments.csv", "this is an assignment file csv".getBytes)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/assignments", check = false)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val zipBytes = r.bytes
    val zipPath = tempFolderPath / "received.zip"
    os.write(zipPath, zipBytes)
    FileUtils.unzip(zipPath, tempFolderPath / "received") match {
      case Some(p) => {
        p should existInFilesystem
        compareFolders(p / Constants.ASSIGNMENTS_DIRECTORY_PATH.segments.toList.last, Constants.ASSIGNMENTS_DIRECTORY_PATH, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
      }
      case None => fail("cannot unzip the received zip")
    }
  }

  // Test of the lock for post endpoints ------------------------------------------------------------------------------------

  "version endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }
    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/version", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "availableProtoDevices endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    mockedSvshi.getAvailableProtoDevices() shouldAnswer ({
      List[String]()
    })

    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/availableProtoDevices", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "listApps endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    mockedSvshi.listApps(*) shouldAnswer ((_: ApplicationLibrary) => {
      Nil
    })
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }
    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/listApps", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "generateApp endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val protoFileName = "test_app_one_proto.json"
    val protoFilePath = pipeline1Path / protoFileName
    val protoJsonString = os.read(protoFilePath)

    val appName = "test_app_one"

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateApp/$appName", data = protoJsonString, check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "compile endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "updateApp endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val appName = "appNameBla42"
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "generateBindings endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    Thread.sleep(1000)
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }
    val existingPhysStructJsonFile = existingAppsLibrary.path / "physical_structure.json"
    val expectedExistingPhysStruct = simpleEtsProjPhysStruct
    PhysicalStructureJsonParser.writeToFile(existingPhysStructJsonFile, expectedExistingPhysStruct)
    val newKnxProjFileZip = simpleEtsProjectZip
    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "removeApp endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val appName = "appTestRemove42"
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/removeApp/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "removeAllApps endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/removeAllApps", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "run endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "runStatus endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }

  }
  "logs/run endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/run", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "logs/receivedTelegrams endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/receivedTelegrams", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "logs/execution endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/logs/execution", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "stopRun endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/stopRun", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  "newApps endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "bindings endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    os.copy(coreResFolderPath / testPhysicalStructureJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)
    os.copy(coreResFolderPath / testBindingsJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME)

    coreResFolderPath / testPhysicalStructureJsonFileName should existInFilesystem
    coreResFolderPath / testPhysicalStructureJsonFileName should beAFile
    coreResFolderPath / testBindingsJsonFileName should existInFilesystem
    coreResFolderPath / testBindingsJsonFileName should beAFile

    val expectedPhysicalStruct = PhysicalStructureJsonParser.parseJson(os.read(coreResFolderPath / testPhysicalStructureJsonFileName))
    val expectedBindings = BindingsJsonParser.parse(coreResFolderPath / testBindingsJsonFileName)

    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "post generated endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "get generated endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "deleteAllGenerated endpoint" should "reply with a 423 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deleteAllGenerated", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 423
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }
  "installedApp endpoint" should "reply with a 200 code when another request (compile) is running" in {
    mockedSvshi.compileApps(*, *, *)(*, *, *, *) shouldAnswer (
      (_: ApplicationLibrary, _: ApplicationLibrary, _: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        Thread.sleep(300)
        0
      }
    )
    val appName = "test_app_one"
    val app1Path = pipeline1Path / "expected_library" / appName
    os.copy.into(app1Path, Constants.INSTALLED_APPS_FOLDER_PATH)

    val knxProjFileZip = simpleEtsProjectZip
    val thread1 = new Thread {
      override def run(): Unit = {
        val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
        r.statusCode shouldEqual 200
      }
    }

    thread1.start()
    Thread.sleep(80) // Important to let time to the thread1 to start
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/installedApp/$appName", check = false, readTimeout = requestsReadTimeout)
    r.statusCode shouldEqual 200
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    eventually(timeout(2 seconds)) {
      thread1.isAlive shouldEqual false
    }
  }

  val channelNameKlix1 = "Channel - MD-2_M-1_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix1 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix1
  )
  val channelNameKlix2 = "Channel - MD-2_M-8_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix2 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff - switch and sensor",
    datatype = DPT1(1),
    ioType = InOut,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix2
  )
  val deviceKlix = PhysicalDevice(
    "KliX (D4)",
    ("1", "1", "1"),
    List(
      PhysicalDeviceNode(
        channelNameKlix1,
        List(
          commObjectKlix1
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix2,
        List(
          commObjectKlix2
        )
      )
    )
  )
  private val physicalStructure = PhysicalStructure(List(deviceKlix))
  val klixMappings = DeviceMapping(
    "1.1.1",
    List(
      SupportedDeviceMappingNode(
        name = channelNameKlix1,
        List(SupportedDeviceMapping(name = commObjectKlix1.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix1.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix2,
        List(SupportedDeviceMapping(name = commObjectKlix2.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix2.id))
      )
    )
  )
  val deviceMappings = StructureMapping(PhysicalStructureJsonParser.physicalStructureToJson(physicalStructure), List(klixMappings))

  "deviceMappings endpoint" should "fail with a file which is not a json nor a knxproj file" in {

    mockedSvshi.generatePrototypicalDeviceMappings(*)(*, *, *, *) shouldAnswer (
      (_: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        deviceMappings.writeToFile(GENERATED_AVAILABLE_PROTODEVICES_FOR_ETS_STRUCT_FILEPATH)
        success("The device mappings were correctly generated!")
        0
      }
    )
    val wrongFilePath = tempFolderPath / "input_json_struct.png"
    os.write(wrongFilePath, "test".getBytes())
    val newKnxProjFileZip = createInMemZip(wrongFilePath)

    val r =
      requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deviceMappings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 400
    r.text() shouldEqual "The file for the physical structure must be either a json or a knxproj file!"
  }

  "deviceMappings endpoint" should "call the Svshi functions with the correct library" in {

    mockedSvshi.generatePrototypicalDeviceMappings(*)(*, *, *, *) shouldAnswer (
      (_: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        deviceMappings.writeToFile(GENERATED_AVAILABLE_PROTODEVICES_FOR_ETS_STRUCT_FILEPATH)
        success("The device mappings were correctly generated!")
        0
      }
    )

    // Create app libraries:
    val knxProjFile = simpleEtsProjFilePath
    val knxProjFileZip = createInMemZip(knxProjFile)

    val captorPhysStruct = ArgCaptor[PhysicalStructure]
    val captorSuccess = ArgCaptor[String => Unit]
    val captorInfo = ArgCaptor[String => Unit]
    val captorWarning = ArgCaptor[String => Unit]
    val captorError = ArgCaptor[String => Unit]

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deviceMappings", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generatePrototypicalDeviceMappings(captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorPhysStruct.value shouldEqual simpleEtsProjPhysStruct
  }

  "deviceMappings endpoint" should "return the structure in the file" in {

    mockedSvshi.generatePrototypicalDeviceMappings(*)(*, *, *, *) shouldAnswer (
      (_: PhysicalStructure, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        deviceMappings.writeToFile(GENERATED_AVAILABLE_PROTODEVICES_FOR_ETS_STRUCT_FILEPATH)
        success("The device mappings were correctly generated!")
        0
      }
    )

    // Create app libraries:
    val knxProjFile = simpleEtsProjFilePath
    val knxProjFileZip = createInMemZip(knxProjFile)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_DEFAULT_PORT}/deviceMappings", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    StructureMapping.parseJson(r.text()) shouldEqual deviceMappings
  }

  private def createInMemZip(file: os.Path): Array[Byte] = {
    val os = new ByteArrayOutputStream()
    val zipOut = new ZipOutputStream(os)
    val fileToZip = file.toIO
    val fis = new FileInputStream(fileToZip)
    val zipEntry = new ZipEntry(fileToZip.getName)
    zipOut.putNextEntry(zipEntry)
    val bytes = new Array[Byte](1024)
    var length = fis.read(bytes)
    while (length >= 0) {
      zipOut.write(bytes, 0, length)
      length = fis.read(bytes)
    }
    zipOut.close()
    fis.close()
    val res = os.toByteArray
    os.close()
    res
  }
}
