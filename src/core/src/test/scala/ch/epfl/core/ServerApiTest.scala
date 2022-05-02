package ch.epfl.core

import ch.epfl.core.CustomMatchers.{beAFile, beSimilarToLibrary, containThePairOfHeaders, existInFilesystem}
import ch.epfl.core.TestUtils.{compareFolders, defaultIgnoredFilesAndDir}
import ch.epfl.core.api.server.CoreApiServer
import ch.epfl.core.api.server.json.{BindingsResponse, ResponseBody}
import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.model.physical.PhysicalStructure
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

  private val app1ProtoStruct = AppInputJsonParser.parse(pipeline1Path / app1ProtoFileName)
  private val app2ProtoStruct = AppInputJsonParser.parse(pipeline1Path / app1ProtoFileName)
  private val existingAppsLibrary = ApplicationLibrary(List(Application("app1", Constants.APP_LIBRARY_FOLDER_PATH / "app1", app1ProtoStruct)), Constants.APP_LIBRARY_FOLDER_PATH)
  private val newAppsLibrary = ApplicationLibrary(List(Application("app2", Constants.APP_LIBRARY_FOLDER_PATH / "app2", app2ProtoStruct)), Constants.GENERATED_FOLDER_PATH)
  private val existingPhysicalStructure = PhysicalStructure(Nil)

  private val backupGeneratedPath = SVSHI_SRC_FOLDER_PATH / "backup_generated_during_test"
  private val backupLibraryPath = SVSHI_SRC_FOLDER_PATH / "backup_library_during_test"
  private val backupInstalledAppsPath = SVSHI_SRC_FOLDER_PATH / "backup_installed_apps_during_test"
  private val backupAssignmentsPath = SVSHI_SRC_FOLDER_PATH / "backup_assignments"

  private val runLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / "private_run_logs.log"

  private val coreResFolderPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res"
  private val coreResApiServerFolderPath = coreResFolderPath / "apiServerTests"

  private val testPhysicalStructureJsonFileName = "test_physicalJson.json"
  private val testBindingsJsonFileName = "test_bindings_json_parser.json"

  private val simpleEtsProjFilePath = coreResFolderPath / "Simple.knxproj"
  private lazy val simpleEtsProjPhysStruct = EtsParser.parseEtsProjectFile(simpleEtsProjFilePath)

  private val pythonScriptSleeps5Secs = "sleep5secsThenExit0.py"
  private val pythonScriptSleeps1Sec = "sleep1secThenExit0.py"

  private val fakeContentDirectoryPath = coreResApiServerFolderPath / "fakeContent"
  private val fakeContentDirectoryZipPath = coreResApiServerFolderPath / "fakeContent.zip"
  private val oldFakeContentDirectoryPath = coreResApiServerFolderPath / "fakeContentBeforeUpload"
  private val fakeContentAfterDirectoryPath = coreResApiServerFolderPath / "fakeContentAfterUpload"

  private val tempFolderPath = coreResApiServerFolderPath / "tempTests"

  private val mockedSvshi = mock[SvshiTr]
  private val coreApiServer = CoreApiServer(mockedSvshi)

  private val expectedHeaders = Seq("Access-Control-Allow-Origin".toLowerCase -> "*")

  private val requestsReadTimeout = 60_000 // Milliseconds

  override def beforeAll(): Unit = {
    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInstalledAppsPath)) os.remove.all(backupInstalledAppsPath)
    if (os.exists(backupAssignmentsPath)) os.remove.all(backupAssignmentsPath)

    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.copy(APP_LIBRARY_FOLDER_PATH, backupLibraryPath)
    if (os.exists(GENERATED_FOLDER_PATH)) os.copy(GENERATED_FOLDER_PATH, backupGeneratedPath)
    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.copy(INSTALLED_APPS_FOLDER_PATH, backupInstalledAppsPath)
    if (os.exists(ASSIGNMENTS_DIRECTORY_PATH)) os.copy(ASSIGNMENTS_DIRECTORY_PATH, backupAssignmentsPath)
  }
  override def afterAll(): Unit = {
    if (os.exists(APP_LIBRARY_FOLDER_PATH)) os.remove.all(APP_LIBRARY_FOLDER_PATH)
    if (os.exists(GENERATED_FOLDER_PATH)) os.remove.all(GENERATED_FOLDER_PATH)
    if (os.exists(INSTALLED_APPS_FOLDER_PATH)) os.remove.all(INSTALLED_APPS_FOLDER_PATH)
    if (os.exists(ASSIGNMENTS_DIRECTORY_PATH)) os.remove.all(ASSIGNMENTS_DIRECTORY_PATH)

    if (os.exists(backupLibraryPath)) os.copy(backupLibraryPath, APP_LIBRARY_FOLDER_PATH)
    if (os.exists(backupGeneratedPath)) os.copy(backupGeneratedPath, GENERATED_FOLDER_PATH)
    if (os.exists(backupInstalledAppsPath)) os.copy(backupInstalledAppsPath, INSTALLED_APPS_FOLDER_PATH)
    if (os.exists(backupAssignmentsPath)) os.copy(backupAssignmentsPath, ASSIGNMENTS_DIRECTORY_PATH)

    if (os.exists(backupLibraryPath)) os.remove.all(backupLibraryPath)
    if (os.exists(backupGeneratedPath)) os.remove.all(backupGeneratedPath)
    if (os.exists(backupInstalledAppsPath)) os.remove.all(backupInstalledAppsPath)
    if (os.exists(backupAssignmentsPath)) os.remove.all(backupAssignmentsPath)

    if (os.exists(tempFolderPath)) os.remove.all(tempFolderPath)
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

    if (os.exists(tempFolderPath)) os.remove.all(tempFolderPath)
    os.makeDir.all(tempFolderPath)

    reset(mockedSvshi)
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

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/version", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generateApp/$appName", data = protoJsonString, check = false, readTimeout = requestsReadTimeout)
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
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/removeApp/$appName", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/removeAllApps", check = false, readTimeout = requestsReadTimeout)
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
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
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
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
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
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
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
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
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
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    eventually(timeout(3 seconds)) {
      val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
      expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
      r.statusCode shouldEqual 200
      val responseBody = ResponseBody.from(r.text())
      responseBody.status shouldBe false
    }
  }

  "runStatus endpoint" should "reply with a false status if run is not running" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
  }

  "runLogs endpoint" should "reply with the content of the log file when run is running" in {

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
    requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)

    eventually(timeout(1 seconds)) {
      val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runLogs", check = false, readTimeout = requestsReadTimeout)
      expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
      r.statusCode shouldEqual 200
      val responseBody = ResponseBody.from(r.text())
      responseBody.status shouldBe true
      responseBody.output.mkString("\n").trim shouldEqual expectedContent
    }
  }

  "runLogs endpoint" should "reply with false status if the run command was never called" in {
    val localRunLogFileName = "private_run_logs.log"
    val localRunLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / localRunLogFileName
    os.remove(localRunLogFilePath)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runLogs", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe false
  }

  "runLogs endpoint" should "reply with true status and the last log if the run command is not running" in {
    val localRunLogFileName = "private_run_logs.log"
    val localRunLogFilePath = Constants.PRIVATE_SERVER_LOG_FOLDER_PATH / localRunLogFileName
    val log = "test log"
    FileUtils.writeToFileOverwrite(localRunLogFilePath, log.getBytes)
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runLogs", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List(log)
  }

  "stopRun endpoint" should "stop the run thread when called and reply with the message" in {

    mockedSvshi.run(*, *, *)(*, *, *, *) shouldAnswer (
      (_: Option[String], _: ApplicationLibrary, _: Boolean, success: String => Unit, info: String => Unit, warning: String => Unit, error: String => Unit) => {
        val p = os.proc("python3", pythonScriptSleeps5Secs).spawn(coreResFolderPath)
        val svshiSubProcess = new SvshiSubProcessOs(p)
        new SvshiRunResult(Some(svshiSubProcess), 0)
      }
    )
    val r1 = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/run/1.1.1.1:3671", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true

    val r2 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r2.headers should containThePairOfHeaders(p))
    r2.statusCode shouldEqual 200
    val responseBody2 = ResponseBody.from(r2.text())
    responseBody2.status shouldBe true

    val r3 = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/stopRun", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r3.headers should containThePairOfHeaders(p))
    r3.statusCode shouldEqual 200
    val responseBody3 = ResponseBody.from(r3.text())
    responseBody3.status shouldBe true
    responseBody3.output.mkString("\n") shouldEqual "SVSHI run was stopped!"

    eventually(timeout(1 seconds)) {
      val r4 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/runStatus", check = false, readTimeout = requestsReadTimeout)
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

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
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

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual List(app1Name, app2Name)
  }

  "newApps endpoint" should "reply with the list of folders in generated when empty" in {

    val r1 = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/newApps", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r1.headers should containThePairOfHeaders(p))
    r1.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r1.text())
    responseBody.status shouldBe true
    responseBody.output shouldEqual Nil
  }

  "compile endpoint" should "call the compile function with the correct physicalStructure" in {

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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).compileApps(captorExistingApps, captorNewApps, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedPhysStruct = simpleEtsProjPhysStruct
    captorPhysStruct hasCaptured expectedPhysStruct

    val expectedOutput = "Applications were installed!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/compile", data = knxProjFileZip, check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/updateApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    verify(mockedSvshi).updateApp(captorExistingApps, captorNewApps, captorAppName, captorPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorPhysStruct.value shouldEqual expectedExistingPhysStruct

    val expectedOutput = f"$appName was updated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "generateBindings endpoint" should "call the generateBindings function with the correct physicalStructures" in {

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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generateBindings(captorExistingApps, captorNewApps, captorExistingPhysStruct, captorNewPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    val expectedNewPhysStruct = simpleEtsProjPhysStruct

    val expectedOutput = "Bindings were generated!"
    val responseBody = ResponseBody.from(r.text())

    captorNewPhysStruct hasCaptured expectedNewPhysStruct
    captorExistingPhysStruct hasCaptured expectedExistingPhysStruct
    responseBody.output.mkString("\n") shouldEqual expectedOutput
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

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generateBindings", data = newKnxProjFileZip, check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    verify(mockedSvshi).generateBindings(captorExistingApps, captorNewApps, captorExistingPhysStruct, captorNewPhysStruct)(captorSuccess, captorInfo, captorWarning, captorError)

    captorNewApps.value should beSimilarToLibrary(newAppsLibrary)
    captorExistingApps.value should beSimilarToLibrary(existingAppsLibrary)

    val expectedOutput = "Bindings were generated!"
    val responseBody = ResponseBody.from(r.text())
    responseBody.output.mkString("\n") shouldEqual expectedOutput
  }

  "bindings endpoint" should "reply with a 404 when no bindings and physicalStructure files are in generated" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "bindings endpoint" should "reply with a 404 when no bindings file is in generated but one physical_structure is" in {
    os.copy(coreResFolderPath / testPhysicalStructureJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME)

    Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME should existInFilesystem
    Constants.GENERATED_FOLDER_PATH / Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME should beAFile
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "bindings endpoint" should "reply with a 404 when no physical_structure file is in generated but one bindings is" in {
    os.copy(coreResFolderPath / testBindingsJsonFileName, Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME)

    Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME should existInFilesystem
    Constants.GENERATED_FOLDER_PATH / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME should beAFile
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/bindings", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val bindingsResponse = BindingsResponse.from(r.text())
    bindingsResponse.bindings shouldEqual expectedBindings
    bindingsResponse.physicalStructure shouldEqual expectedPhysicalStruct
  }

  "generated post endpoint" should "move all files to generated folder when it is empty before the operation and send list in body" in {
    val data = os.read.bytes(fakeContentDirectoryZipPath)
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generated", data = data)
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
    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generated", data = data)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))

    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldBe true

    compareFolders(folder1 = fakeContentAfterDirectoryPath, folder2 = Constants.GENERATED_FOLDER_PATH, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
  }

  "deleteGenerated endpoint" should "remove everything that is contained in the generated folder" in {
    os.makeDir.all(Constants.GENERATED_FOLDER_PATH / "aDir")
    FileUtils.writeToFileOverwrite(Constants.GENERATED_FOLDER_PATH / "aFile", "test".getBytes)

    val r = requests.post(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/deleteGenerated")
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 200
    val responseBody = ResponseBody.from(r.text())
    responseBody.status shouldEqual true
    responseBody.output should contain("The generated folder was successfully emptied!")

    os.list(Constants.GENERATED_FOLDER_PATH).toList shouldEqual Nil
  }

  "installedApp get appName endpoint" should "reply with a 404 if the requested app is not installed" in {
    val appName = "notInstalled"
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/installedApp/$appName", check = false, readTimeout = requestsReadTimeout)
    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
    r.statusCode shouldEqual 404
  }

  "installedApp get appName endpoint" should "reply with a zip of the requested app in the body if it is installed" in {
    // Prepare
    val appName = "test_app_one"
    val app1Path = pipeline1Path / "expected_library" / appName
    os.copy.into(app1Path, Constants.INSTALLED_APPS_FOLDER_PATH)

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/installedApp/$appName", check = false, readTimeout = requestsReadTimeout)
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

  "generated get endpoint" should "reply with the content of the generated folder in a zip when empty" in {
    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
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

    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/generated", check = false, readTimeout = requestsReadTimeout)
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

// BREAKS THE OTHER TESTS! Probably problem with the mocks, these are the only tests where
// the mock returns something different than an Int

//  "generateApp/availableDevices endpoint" should "return the available devices for new apps" in {
//
//    val expectedDevices = List("device1", "device2")
//    mockedSvshi.getAvailableProtoDevices() shouldReturn expectedDevices
//
//    mockedSvshi.getAvailableProtoDevices() shouldEqual expectedDevices
//    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/availableProtoDevices")
//    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
//    r.statusCode shouldEqual 200
//    val responseBody = ResponseBody.from(r.text())
//    responseBody.status shouldBe true
//
//    responseBody.outputLines should contain theSameElementsAs expectedDevices
//  }
//  "listApps endpoint" should "return the correct list with the installed apps in the outputs" in {
//
//    mockedSvshi.listApps(*) shouldAnswer ((_: ApplicationLibrary) => {
//      List("app1", "app2")
//    })
//
//    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/listApps", check = false, readTimeout = requestsReadTimeout)
//    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
//    r.statusCode shouldEqual 200
//    val responseBody = ResponseBody.from(r.text())
//
//    responseBody.status shouldEqual true
//    val expectedOutput = "app1\napp2"
//    responseBody.outputLines.mkString("\n") shouldEqual expectedOutput
//  }
//
//  "listApps endpoint" should "return an empty list when no apps are installed" in {
//
//    mockedSvshi.listApps(*) shouldAnswer ((_: ApplicationLibrary) => {
//      Nil
//    })
//
//    val r = requests.get(f"http://localhost:${Constants.SVSHI_GUI_SERVER_PORT}/listApps", check = false, readTimeout = requestsReadTimeout)
//    expectedHeaders.foreach(p => r.headers should containThePairOfHeaders(p))
//    r.statusCode shouldEqual 200
//    val responseBody = ResponseBody.from(r.text())
//
//    responseBody.status shouldEqual true
//    responseBody.outputLines shouldEqual Nil
//  }

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
