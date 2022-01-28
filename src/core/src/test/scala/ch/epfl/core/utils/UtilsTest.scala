package ch.epfl.core.utils

import ch.epfl.core.utils.Constants.SVSHI_SRC_FOLDER_PATH
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UtilsTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  private val resFolderPath = SVSHI_SRC_FOLDER_PATH / "core" / "res"
  private val testFilePath = resFolderPath / "test_get_lines.py"

  "getLineNFile" should "return the 2nd line with the correct format" in {
    val n = 2
    val line = ""
    val expected = s"$n:  $line"
    Utils.getLineNFile(testFilePath, n) shouldEqual Some(expected)
  }

  "getLineNFile" should "return the 3rd line with the correct format" in {
    val n = 3
    val line = "def getLinesTest():"
    val expected = s"$n:  $line"
    Utils.getLineNFile(testFilePath, n) shouldEqual Some(expected)
  }

  "getLineNFile" should "return the 7th line with the correct format" in {
    val n = 7
    val line = "    a = 1"
    val expected = s"$n:  $line"
    Utils.getLineNFile(testFilePath, n) shouldEqual Some(expected)
  }

  "getLineNFile" should "return None if the file does not exist" in {
    Utils.getLineNFile(resFolderPath / "test_get_lines_does_not_exist.py", 2) shouldEqual None
  }

  "getLineNFile" should "return None if the file has fewer lines than n" in {
    Utils.getLineNFile(testFilePath, 42) shouldEqual None
  }

}
