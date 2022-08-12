package ch.epfl.web.service.main.session

import ch.epfl.web.service.main.CustomMatchers.{existInFilesystem, haveSameContentAsIgnoringBlanks}
import ch.epfl.web.service.main.TestUtils
import ch.epfl.web.service.main.svshi.SvshiInterface
import ch.epfl.web.service.main.utils.{Constants, FileUtils}
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito.StubbingOps
import org.mockito.MockitoSugar.{mock, reset}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, defined, empty, have, not}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import java.time.Instant

class SessionTest extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  private val resTestFolderPath = os.pwd / "res" / "test"
  private val resMocksFolderPath = os.pwd / "res" / "mocks"

  private val svshiIntMock = mock[SvshiInterface]

  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = {
    if (os.exists(Constants.TEMP_FOLDER_PATH)) os.remove.all(Constants.TEMP_FOLDER_PATH)
  }

  override def beforeEach(): Unit = {
    if (os.exists(Constants.TEMP_FOLDER_PATH)) os.remove.all(Constants.TEMP_FOLDER_PATH)
    if (os.exists(Constants.SESSIONS_FOLDER_PATH)) os.remove.all(Constants.SESSIONS_FOLDER_PATH)

    reset(svshiIntMock)

  }

  "setEtsFile" should "remove the mappings and the parsed structure" in {

    svshiIntMock.getDeviceMappings(*) shouldAnswer ((etsProjFilePath: os.Path) => {
      FileUtils.readFileContentAsString(resMocksFolderPath / "available_proto_devices.json")
    })
    svshiIntMock.getParserPhysicalStructureJson(*) shouldAnswer ((etsZip: Array[Byte]) => {
      FileUtils.readFileContentAsString(resMocksFolderPath / "physical_structure.json")
    })

    val id = "424242"
    val session = new Session(id)
    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    session.getOrComputeDeviceMappings(svshiIntMock)
    session.getOrComputePhysicalStructureJson(svshiIntMock)

    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    session.privateFolderPath / "mappings" / "deviceMappings.json" shouldNot existInFilesystem
    session.privateFolderPath / "physicalStructure" / "physicalStructure.json" shouldNot existInFilesystem

  }

  "getOrComputeDeviceMappings" should "correctly send the request and store the file if mappings not yet defined" in {

    svshiIntMock.getDeviceMappings(*) shouldAnswer ((etsProjFilePath: os.Path) => {
      FileUtils.readFileContentAsString(resMocksFolderPath / "available_proto_devices.json")
    })

    val id = "424242"
    val session = new Session(id)
    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    FileUtils.getListOfFiles(session.privateFolderPath / "mappings") shouldBe empty
    val json = session.getOrComputeDeviceMappings(svshiIntMock)
    FileUtils.getListOfFiles(session.privateFolderPath / "mappings") should not be empty

    json shouldEqual FileUtils.readFileContentAsString(resMocksFolderPath / "available_proto_devices.json")
  }

  "getOrComputeDeviceMappings" should "return the file content if mappings are defined without calling the svshi interface" in {

    svshiIntMock.getDeviceMappings(*) shouldAnswer ((etsProjFilePath: os.Path) => {
      "THE SESSION SHOULD NOT HAVE CALLED SVSHI!"
    })

    val id = "424242"
    val session = new Session(id)
    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    os.copy(resMocksFolderPath / "available_proto_devices.json", session.privateFolderPath / "mappings" / "deviceMappings.json")

    FileUtils.getListOfFiles(session.privateFolderPath / "mappings") should not be empty
    val json = session.getOrComputeDeviceMappings(svshiIntMock)
    FileUtils.getListOfFiles(session.privateFolderPath / "mappings") should not be empty

    json shouldEqual FileUtils.readFileContentAsString(resMocksFolderPath / "available_proto_devices.json")

  }

  "reloadFromFile" should "create the correct floor map using files and app map" in {
    val id = "4242"
    val session = new Session(id)
    val file = resTestFolderPath / "test_cad_file.dxf"
    session.addFloor(0, "floor1", file)
    session.addFloor(1, "floor2", file)

    session.addApp("app_one", resMocksFolderPath / "app_one_generated")

    val session2 = new Session(id)
    session2.reloadFromFile()
    session2.id shouldEqual id
    session2.getFloors() should have length 2
    val floor1 = session2.getFloor(0)
    floor1 shouldBe defined
    floor1.get.number shouldEqual 0
    floor1.get.name shouldEqual "floor1"
    floor1.get.blueprintFilePath should haveSameContentAsIgnoringBlanks(file)

    val floor2 = session2.getFloor(1)
    floor2 shouldBe defined
    floor2.get.number shouldEqual 1
    floor2.get.name shouldEqual "floor2"
    floor2.get.blueprintFilePath should haveSameContentAsIgnoringBlanks(file)

    session2.appNamesList should contain only "app_one"

    FileUtils.copyDirWithNewName(resMocksFolderPath / "app_one_generated", Constants.TEMP_FOLDER_PATH, "app_one")

    val zipOpt = session2.getZipForAppFiles("app_one")
    val expected = FileUtils.zipInMem(List(Constants.TEMP_FOLDER_PATH / "app_one")).get
    zipOpt shouldBe defined
    zipOpt.get shouldEqual expected
  }

  "renewLifespan" should "make a expired session valid again" in {
    val session = new Session("424242")
    session.setExpiration(Instant.now().minusSeconds(40_000))
    session.isExpired shouldBe true

    session.renewLifespan()
    session.isExpired shouldBe false
  }

  "addApp" should "add a new app to the session list and copy the files over" in {
    val id = "424242"
    val session = new Session(id)

    session.addApp("app_one", resMocksFolderPath / "app_one_generated")

    session.appNamesList should contain only "app_one"
    session.privateFolderPath / "applications" should existInFilesystem
    session.privateFolderPath / "applications" / "app_one" should existInFilesystem
    TestUtils.compareFolders(
      session.privateFolderPath / "applications" / "app_one",
      resMocksFolderPath / "app_one_generated",
      TestUtils.defaultIgnoredFilesAndDir
    )
  }

  "removeApp" should "remove the app from the map and delete the files" in {
    val id = "424242"
    val session = new Session(id)

    session.addApp("app_one", resMocksFolderPath / "app_one_generated")

    session.appNamesList should contain only "app_one"
    session.removeApp("app_one")
    session.appNamesList shouldBe empty
    session.privateFolderPath / "applications" / "app_one" shouldNot existInFilesystem
  }

  "getZipForAppFiles" should "return None if app not in session" in {
    val id = "424242"
    val session = new Session(id)

    session.getZipForAppFiles("helloThere") shouldBe None

  }

  "getZipForAppFiles" should "return the correct zip if app in session" in {
    val id = "424242"
    val session = new Session(id)

    session.addApp("app_one", resMocksFolderPath / "app_one_generated")

    FileUtils.copyDirWithNewName(resMocksFolderPath / "app_one_generated", Constants.TEMP_FOLDER_PATH, "app_one")

    val zipOpt = session.getZipForAppFiles("app_one")
    val expected = FileUtils.zipInMem(List(Constants.TEMP_FOLDER_PATH / "app_one")).get
    zipOpt shouldBe defined
    zipOpt.get shouldEqual expected

  }

  "getOrComputePhysicalStructureJson" should "correctly send the request and store the file if structure not yet defined" in {

    svshiIntMock.getParserPhysicalStructureJson(*) shouldAnswer ((etsZip: Array[Byte]) => {
      FileUtils.readFileContentAsString(resMocksFolderPath / "physical_structure.json")
    })

    val id = "424242"
    val session = new Session(id)
    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    FileUtils.getListOfFiles(session.privateFolderPath / "physicalStructure") shouldBe empty
    val json = session.getOrComputePhysicalStructureJson(svshiIntMock)
    FileUtils.getListOfFiles(session.privateFolderPath / "physicalStructure") should not be empty

    json shouldEqual FileUtils.readFileContentAsString(resMocksFolderPath / "physical_structure.json")
  }

  "getOrComputePhysicalStructureJson" should "return the file content if structure are defined without calling the svshi interface" in {

    svshiIntMock.getParserPhysicalStructureJson(*) shouldAnswer ((etsZip: Array[Byte]) => {
      "THE SESSION SHOULD NOT HAVE CALLED SVSHI!"
    })

    val id = "424242"
    val session = new Session(id)
    session.setEtsFile(resTestFolderPath / "DSLAB_proto.knxproj")
    session.hasEtsFile shouldBe true

    os.copy(resMocksFolderPath / "physical_structure.json", session.privateFolderPath / "physicalStructure" / "physicalStructure.json")

    FileUtils.getListOfFiles(session.privateFolderPath / "physicalStructure") should not be empty
    val json = session.getOrComputePhysicalStructureJson(svshiIntMock)
    FileUtils.getListOfFiles(session.privateFolderPath / "physicalStructure") should not be empty

    json shouldEqual FileUtils.readFileContentAsString(resMocksFolderPath / "physical_structure.json")

  }

}
