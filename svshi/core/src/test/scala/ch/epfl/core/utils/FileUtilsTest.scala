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
//    val wd = os.pwd / os.up / os.up
//    val outputPath = os.Path("temp", wd)
//    val inputPath = os.Path(testFilePathString, wd)
//    FileUtils.unzip(inputPath, outputPath)
//    val l = FileUtils.recursiveListFiles(outputPath)
//    val refPath = os.Path(testFilePathUnzippedString, wd)
//    val lRef = FileUtils.recursiveListFiles(refPath)
//    l.size shouldEqual lRef.size
//    for(e <- l){
//      print(e)
//      lRef.contains(e) shouldEqual true
//    }
//    os.remove.all(outputPath)
  }

}
