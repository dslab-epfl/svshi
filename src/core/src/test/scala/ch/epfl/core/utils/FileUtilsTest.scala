package ch.epfl.core.utils

import ch.epfl.core.CustomMatchers.{existInFilesystem, haveSameContentAsIgnoringBlanks}
import ch.epfl.core.TestUtils.{compareFolders, defaultIgnoredFilesAndDir}
import ch.epfl.core.utils.Constants.SVSHI_SRC_FOLDER_PATH
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import os.Path

class FileUtilsTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  override def beforeEach(): Unit = {
    os.remove.all(outputPath)
  }
  override def afterEach(): Unit = {
    if (os.exists(outputPath)) os.remove.all(outputPath)
  }

  val testFilePathString = "core/res/ets_project_test.knxproj"
  val macosxZipFilePathString = "core/res/fileUtilsTest/zipWithMACOSX.zip"
  val macosxZipFileUnzippedWithoutMacosxPathString = "core/res/fileUtilsTest/zipWithMACOSX.zip"
  val testFileRefPathUnzippedString = "core/res/ets_project_test"
  val outputPath: Path = os.Path("core/res/temp", SVSHI_SRC_FOLDER_PATH)
  val resFolderPath = Constants.SVSHI_SRC_FOLDER_PATH / "core" / "res" / "fileUtilsTest"

  val wd = SVSHI_SRC_FOLDER_PATH
  val ignoredFilesName = List(".DS_Store")

  "unzip" should "unzip all files" in {
    os.remove.all(outputPath)
    val inputPath = os.Path(testFilePathString, wd)
    if (!os.exists(inputPath)) fail("The input file does not exist!")
    FileUtils.unzip(inputPath, outputPath)
    val l = FileUtils.recursiveListFiles(outputPath).filterNot(p => ignoredFilesName.exists(f => p.toString().contains(f)))
    val refPath = os.Path(testFileRefPathUnzippedString, wd)
    val lRef = FileUtils.recursiveListFiles(refPath).filterNot(p => ignoredFilesName.exists(f => p.toString().contains(f)))
    l.size shouldEqual lRef.size
    for (e <- l) {
      lRef.map(f => f.relativeTo(refPath)) should contain(e.relativeTo(outputPath))
      if (os.isFile(e)) {
        e should haveSameContentAsIgnoringBlanks(lRef.find(p => p.relativeTo(refPath) == e.relativeTo(outputPath)).get)
      }
    }
    for (e <- lRef) {
      l.map(f => f.relativeTo(outputPath)) should contain(e.relativeTo(refPath))
    }
  }

  "unzip" should "unzip all files except __MACOSX" in {
    os.remove.all(outputPath)
    val inputPath = os.Path(macosxZipFilePathString, wd)
    if (!os.exists(inputPath)) fail("The input file does not exist!")
    FileUtils.unzip(inputPath, outputPath)
    os.list(outputPath).toList.length shouldEqual 1
    os.list(outputPath).toList.exists(p => p.segments.toList.contains("__MACOSX")) shouldBe false

  }

  "unzip" should "create the output folder even if the zip is empty" in {
    FileUtils.zip(Nil, outputPath / "empty.zip") match {
      case Some(emptyZip) =>
        FileUtils.unzip(emptyZip, outputPath / "unzipped") match {
          case Some(unzipped) => unzipped should existInFilesystem
          case None           => fail("cannot unzip the empty zip")
        }
      case None => fail("cannot zip an empty list")
    }
  }
  "copyFiles" should "copy all given files" in {
    val resFolder = s"${Constants.SVSHI_SRC_FOLDER}/core/res"
    val testFolder = s"$resFolder/test"
    val outputPath = os.Path(testFolder, wd)
    val firstTestFilePath = os.Path(s"$resFolder/f1.txt", wd)
    val secondTestFilePath = os.Path(s"$resFolder/f2.txt", wd)
    os.write(firstTestFilePath, "A test")
    os.write(secondTestFilePath, "A test")
    os.remove.all(outputPath)
    os.makeDir.all(outputPath)
    val inputPath = os.Path(testFilePathString, wd)
    FileUtils.copyFiles(List(firstTestFilePath, secondTestFilePath), outputPath)
    val l = FileUtils.recursiveListFiles(outputPath)
    val refPathFirst = os.Path(s"$testFolder/f1.txt", wd)
    val refPathSecond = os.Path(s"$testFolder/f2.txt", wd)
    l.contains(refPathFirst) shouldEqual true
    l.contains(refPathSecond) shouldEqual true
    os.remove.all(outputPath)
    os.remove.all(firstTestFilePath)
    os.remove.all(secondTestFilePath)
  }

  "checkSizeAndReduce" should "throw an IllegalArgumentException if the file does not exist" in {
    val file = outputPath / "does_not_exist.txt"
    a[IllegalArgumentException] shouldBe thrownBy(FileUtils.checkSizeAndReduce(file, 42))
  }

  "checkSizeAndReduce" should "throw an IllegalArgumentException if the maxSizeBytes is smaller than 0" in {
    val file = outputPath / "exists.txt"
    FileUtils.writeToFileOverwrite(file, "test".getBytes)
    a[IllegalArgumentException] shouldBe thrownBy(FileUtils.checkSizeAndReduce(file, -42))
  }

  "checkSizeAndReduce" should "do nothing if actual file size smaller" in {
    val file = outputPath / "exists.txt"
    FileUtils.writeToFileOverwrite(file, "test".getBytes)
    val oldContent = os.read(file)
    FileUtils.checkSizeAndReduce(file, 20 * 1024)
    os.read(file) shouldEqual oldContent
  }

  "checkSizeAndReduce" should "remove the first 2000 lines if the file is bigger than the threshold" in {
    val file = outputPath / "exists.txt"
    FileUtils.writeToFileOverwrite(file, "".getBytes)
    val maxSize = 200 * 1024
    var i = 0
    val step = 2000
    do {
      val s = (i until i + step).map(index => f"$index: this is a line written to the file.\n").mkString
      FileUtils.writeToFileAppend(file, s.getBytes)
      i += step
    } while (os.size(file) <= maxSize)
    val oldContent = os.read.lines(file)
    FileUtils.checkSizeAndReduce(file, maxSize)
    os.read.lines(file).toList shouldEqual oldContent.toList.to(LazyList).drop(2000).toList
  }

  "zip" should "throw a new IllegalArgumentException when the outputZip parameter is a directory" in {
    val dirPath = outputPath / "aDirectory"
    os.makeDir.all(dirPath)
    an[IllegalArgumentException] shouldBe thrownBy(FileUtils.zip(Nil, dirPath))
  }

  "zip" should "throw a new IllegalArgumentException when one of the file in the toZip list parameter does not exist" in {
    val outputZip = outputPath / "output"
    val notExistingFilePath = outputPath / "file"
    an[IllegalArgumentException] shouldBe thrownBy(FileUtils.zip(List(notExistingFilePath), outputZip))
  }

  "zip" should "create an archive containing given files and folders that give them back once unarchived and return its path" in {
    val dirToZipPath = resFolderPath / "dir1"
    val fileToZipPath = resFolderPath / "file0.txt"

    val outZip = outputPath / "new_zip.zip"

    val expectedUnzipped = resFolderPath / "expectedUnzipped"
    val unzipped = outputPath / "unzipped"
    val outputZip = FileUtils.zip(List(dirToZipPath, fileToZipPath), outZip)
    outputZip shouldEqual Some(outZip)

    FileUtils.unzip(outZip, unzipped)

    compareFolders(unzipped, expectedUnzipped, ignoredFileAndDirNames = defaultIgnoredFilesAndDir)
  }
}
