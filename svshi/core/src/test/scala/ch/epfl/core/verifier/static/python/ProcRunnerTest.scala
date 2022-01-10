import ch.epfl.core.utils.Constants
import ch.epfl.core.verifier.static.python.ProcRunner.callPython
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProcRunnerTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  "callPython" should "return the exit code 1 and the two lines of text when calling the failingPythonSubProcess" in {
    val wd = Constants.SVSHI_FOLDER_PATH / "core"
    val (resCode, output) = callPython("res.failingPythonSubprocess", wd)
    resCode shouldEqual 1
    output.length shouldEqual 2
    output.head shouldEqual "this a line of error"
    output.tail.head shouldEqual "this is a second line"
  }
}
