package ch.epfl.core.utils

import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileUtilsTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  override def beforeEach(): Unit = {
    Constants.setSvshiHome("../..")
  }
  val testFilePathString = "svshi/core/res/ets_project_test.knxproj"
  val testFilePathUnzippedString = "svshi/core/res/ets_project_test"

  "unzip" should "unzip all files" in {
    val outputPath = os.Path("temp", os.pwd)
    FileUtils.unzip(os.Path(testFilePathString, os.pwd), outputPath)
    val l = FileUtils.recursiveListFiles(outputPath)
    val refPath = os.Path(testFilePathUnzippedString, os.pwd)
    val lRef = FileUtils.recursiveListFiles(refPath)
    l should contain theSameElementsAs lRef
    lRef should contain theSameElementsAs l
    os.remove.all(outputPath)
  }

}
