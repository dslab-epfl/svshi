package ch.epfl.web.service.main.utils

import ch.epfl.web.service.main.CustomMatchers.existInFilesystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{be, defined}
import org.scalatest.matchers.should.Matchers.{an, convertToAnyShouldWrapper}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class FileUtilsTest extends AnyFlatSpec with BeforeAndAfterEach with BeforeAndAfterAll {
  private val resTestFolderPath = os.pwd / "res" / "test"
  private val resMockFolderPath = os.pwd / "res" / "mocks"
  private val tempFolder = resTestFolderPath / "TEMP"

  override def afterAll(): Unit = {
    if (os.exists(tempFolder)) os.remove.all(tempFolder)
  }
  override def afterEach(): Unit = {
    if (os.exists(tempFolder)) os.remove.all(tempFolder)
  }
  override def beforeEach(): Unit = {
    os.makeDir.all(tempFolder)
  }

  "zip dezip" should "be reflective" in {
    val t = FileUtils.zip(List(resTestFolderPath / "DSLAB_proto.knxproj", resTestFolderPath / "ets_project_test.knxproj"), tempFolder / "zip.zip")
    t shouldBe defined

    t.get should existInFilesystem

    FileUtils.unzip(t.get, tempFolder / "unzipped")

    tempFolder / "unzipped" / "DSLAB_proto.knxproj" should existInFilesystem
    tempFolder / "unzipped" / "ets_project_test.knxproj" should existInFilesystem

    os.read.bytes(tempFolder / "unzipped" / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")
    os.read.bytes(tempFolder / "unzipped" / "ets_project_test.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "ets_project_test.knxproj")
  }

  "moveAllFileToOtherDirectory" should "move files" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.copy.into(resTestFolderPath / "ets_project_test.knxproj", f1)

    FileUtils.moveAllFileToOtherDirectory(f1, f2)
    f1 / "DSLAB_proto.knxproj" shouldNot existInFilesystem
    f1 / "ets_project_test.knxproj" shouldNot existInFilesystem

    f2 / "DSLAB_proto.knxproj" should existInFilesystem
    f2 / "ets_project_test.knxproj" should existInFilesystem

    os.read.bytes(f2 / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")
    os.read.bytes(f2 / "ets_project_test.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "ets_project_test.knxproj")

  }

  "moveAllFileToOtherDirectory" should "throws if dest is a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.copy.into(resTestFolderPath / "ets_project_test.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.moveAllFileToOtherDirectory(f1, f2 / "DSLAB_proto.knxproj")
  }
  "moveAllFileToOtherDirectory" should "throws if source is a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.copy.into(resTestFolderPath / "ets_project_test.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.moveAllFileToOtherDirectory(f1 / "DSLAB_proto.knxproj", f2)
  }

  "copyFiles" should "copy files" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.copy.into(resTestFolderPath / "ets_project_test.knxproj", f1)

    FileUtils.copyFiles(List(f1 / "DSLAB_proto.knxproj", f1 / "ets_project_test.knxproj"), f2)
    f1 / "DSLAB_proto.knxproj" should existInFilesystem
    f1 / "ets_project_test.knxproj" should existInFilesystem

    f2 / "DSLAB_proto.knxproj" should existInFilesystem
    f2 / "ets_project_test.knxproj" should existInFilesystem

    os.read.bytes(f2 / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(f1 / "DSLAB_proto.knxproj")
    os.read.bytes(f2 / "ets_project_test.knxproj") shouldEqual os.read.bytes(f1 / "ets_project_test.knxproj")

  }
  "copyFiles" should "throws if dest is a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.copy.into(resTestFolderPath / "ets_project_test.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyFiles(
      List(f1 / "DSLAB_proto.knxproj", f1 / "ets_project_test.knxproj"),
      f2 / "DSLAB_proto.knxproj"
    )
  }

  "copyFile" should "copy the file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    FileUtils.copyFile(f1 / "DSLAB_proto.knxproj", f2)
    f1 / "DSLAB_proto.knxproj" should existInFilesystem

    f1 / "DSLAB_proto.knxproj" should existInFilesystem

    os.read.bytes(f2 / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(f1 / "DSLAB_proto.knxproj")

  }

  "copyFile" should "throws if dest is a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyFile(f1, f2 / "DSLAB_proto.knxproj")
  }
  "copyFile" should "throws if source is not a file or does not exist" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyFile(f1, f2)
    an[IllegalArgumentException] should be thrownBy FileUtils.copyFile(f1 / "does-not-exist", f2)
  }

  "copyFileWithNewName" should "copy the file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    FileUtils.copyFileWithNewName(f1 / "DSLAB_proto.knxproj", f2, "newFile")
    f1 / "DSLAB_proto.knxproj" should existInFilesystem

    f2 / "newFile" should existInFilesystem

    os.read.bytes(f1 / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(f2 / "newFile")

  }

  "copyFileWithNewName" should "throws if dest is a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyFileWithNewName(f1, f2 / "DSLAB_proto.knxproj", "newFile")
  }
  "copyFileWithNewName" should "throws if source is not a file or does not exist" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyFileWithNewName(f1, f2, "newFile")
    an[IllegalArgumentException] should be thrownBy FileUtils.copyFileWithNewName(f1 / "does-not-exist", f2, "newFile")
  }

  "getFileExtension" should "return the extension or empty str if not a file" in {
    FileUtils.getFileExtension(resTestFolderPath / "DSLAB_proto.knxproj") shouldEqual "knxproj"
    FileUtils.getFileExtension(resTestFolderPath) shouldEqual ""
  }

  "copyFileWithNewName" should "copy all content and rename the dir" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.makeDir.all(f1 / "dir1")
    FileUtils.writeToFileOverwrite(f1 / "dir1" / "file11.txt", "hello there!!".getBytes)

    FileUtils.copyDirWithNewName(f1, f2, "newDirName")

    f1 should existInFilesystem
    f1 / "DSLAB_proto.knxproj" should existInFilesystem
    f1 / "dir1" should existInFilesystem
    f1 / "dir1" / "file11.txt" should existInFilesystem

    f2 / "newDirName" should existInFilesystem
    f2 / "newDirName" / "DSLAB_proto.knxproj" should existInFilesystem
    os.read.bytes(f2 / "newDirName" / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")
    f2 / "newDirName" / "dir1" should existInFilesystem
    f2 / "newDirName" / "dir1" / "file11.txt" should existInFilesystem
    FileUtils.readFileContentAsString(f2 / "newDirName" / "dir1" / "file11.txt") shouldEqual "hello there!!"

  }

  "copyFileWithNewName" should "copy all content and rename the dir even if destinationDir does not exist" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.makeDir.all(f1 / "dir1")
    FileUtils.writeToFileOverwrite(f1 / "dir1" / "file11.txt", "hello there!!".getBytes)

    FileUtils.copyDirWithNewName(f1, f2, "newDirName")

    f2 / "newDirName" should existInFilesystem
    f2 / "newDirName" / "DSLAB_proto.knxproj" should existInFilesystem
    os.read.bytes(f2 / "newDirName" / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")
    f2 / "newDirName" / "dir1" should existInFilesystem
    f2 / "newDirName" / "dir1" / "file11.txt" should existInFilesystem
    FileUtils.readFileContentAsString(f2 / "newDirName" / "dir1" / "file11.txt") shouldEqual "hello there!!"

  }

  "copyFileWithNewName" should "replace the current existing dir with same dest name and move all content" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    os.copy.into(resTestFolderPath / "DSLAB_proto.knxproj", f1)
    os.makeDir.all(f1 / "dir1")
    FileUtils.writeToFileOverwrite(f1 / "dir1" / "file11.txt", "hello there!!".getBytes)
    os.makeDir(f2 / "newDirName")
    FileUtils.writeToFileOverwrite(f2 / "newDirName" / "old.txt", "I am your father!".getBytes)

    FileUtils.copyDirWithNewName(f1, f2, "newDirName")

    f2 / "newDirName" should existInFilesystem
    f2 / "newDirName" / "old.txt" shouldNot existInFilesystem
    f2 / "newDirName" / "DSLAB_proto.knxproj" should existInFilesystem
    os.read.bytes(f2 / "newDirName" / "DSLAB_proto.knxproj") shouldEqual os.read.bytes(resTestFolderPath / "DSLAB_proto.knxproj")
    f2 / "newDirName" / "dir1" should existInFilesystem
    f2 / "newDirName" / "dir1" / "file11.txt" should existInFilesystem
    FileUtils.readFileContentAsString(f2 / "newDirName" / "dir1" / "file11.txt") shouldEqual "hello there!!"

  }

  "copyFileWithNewName" should "throw IllegalArgument when dirPath does not exist" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f2)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyDirWithNewName(f1, f2, "newDirName")
  }

  "copyFileWithNewName" should "throw IllegalArgument when dirPath points to a file" in {
    val f1 = tempFolder / "f1"
    val f2 = tempFolder / "f2"
    os.makeDir.all(f1)
    os.makeDir.all(f2)
    FileUtils.writeToFileOverwrite(f1 / "f.txt", "hello there!".getBytes)

    an[IllegalArgumentException] should be thrownBy FileUtils.copyDirWithNewName(f1 / "f.txt", f2, "newDirName")
  }
}
