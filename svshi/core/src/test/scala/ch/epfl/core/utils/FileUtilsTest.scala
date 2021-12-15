package ch.epfl.core.utils

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import os.Path

class FileUtilsTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  override def beforeEach(): Unit = {
    Constants.setSvshiHome("../..")
    os.remove.all(outputPath)
  }
  val testFilePathString = "svshi/core/res/ets_project_test.knxproj"
  val testFilePathUnzippedString = "svshi/core/res/ets_project_test"
  val wd: Path = os.pwd / os.up / os.up
  val outputPath: Path = os.Path("svshi/core/res/temp", wd)

  "unzip" should "unzip all files" in {

    val outputPath = os.Path("temp", wd)
    os.remove.all(outputPath)
    val inputPath = os.Path(testFilePathString, wd)
    FileUtils.unzip(inputPath, outputPath)
    val l = FileUtils.recursiveListFiles(outputPath)
    val refPath = os.Path(testFilePathUnzippedString, wd)
    val lRef = FileUtils.recursiveListFiles(refPath)
    l.size shouldEqual lRef.size
    for (e <- l) {
      lRef.map(f => f.relativeTo(inputPath)).contains(e.relativeTo(outputPath))
    }
    for (e <- lRef) {
      l.map(f => f.relativeTo(outputPath)).contains(e.relativeTo(inputPath))
    }
    os.remove.all(outputPath)
  }

}
