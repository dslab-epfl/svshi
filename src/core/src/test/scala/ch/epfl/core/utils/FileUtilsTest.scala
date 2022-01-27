package ch.epfl.core.utils

import ch.epfl.core.CustomMatchers.haveSameContentAsIgnoringBlanks
import ch.epfl.core.utils.Constants.SVSHI_HOME_PATH
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import os.Path

class FileUtilsTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  override def beforeEach(): Unit = {
    os.remove.all(outputPath)
  }
  val testFilePathString = "svshi/core/res/ets_project_test.knxproj"
  val testFileRefPathUnzippedString = "svshi/core/res/ets_project_test"
  val outputPath: Path = os.Path("svshi/core/res/temp", SVSHI_HOME_PATH)
  val wd = SVSHI_HOME_PATH

  "unzip" should "unzip all files" in {
    val outputPath = os.Path("temp", wd)
    os.remove.all(outputPath)
    val inputPath = os.Path(testFilePathString, wd)
    FileUtils.unzip(inputPath, outputPath)
    val l = FileUtils.recursiveListFiles(outputPath)
    val refPath = os.Path(testFileRefPathUnzippedString, wd)
    val lRef = FileUtils.recursiveListFiles(refPath)
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

    os.remove.all(outputPath)
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
}
