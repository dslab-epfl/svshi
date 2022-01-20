import ch.epfl.core.utils.Constants
import ch.epfl.core.verifier.static.python.ProcRunner.callPython
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProcRunnerTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  "callPython" should "return the exit code 1 and the two lines of text when calling the failingPythonSubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val (resCode, output) = callPython(None, None, "res.failingPythonSubprocess", wd)
    resCode shouldEqual 1
    output.length shouldEqual 2
    output.head shouldEqual "this a line of error"
    output.tail.head shouldEqual "this is a second line"
  }
  "callPython" should "call the callbacks for stdOut when calling the printLinesOutAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    val (resCode, _) = callPython(Some(stdOut), None, "res.printLinesOutAndExit0SubProcess", wd)
    resCode shouldEqual 0
    outL.length shouldEqual 3
    outL.head shouldEqual "this a line of print on stdout"
    outL.tail.head shouldEqual "this is a second line"
    outL.tail.tail.head shouldEqual "this is a third line"
  }

  "callPython" should "call the callbacks for stdErr when calling the printLinesErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdErr = (s: String) => {
      outL = outL ++ List(s)
    }
    val (resCode, _) = callPython(None, Some(stdErr), "res.printLinesErrAndExit0SubProcess", wd)
    resCode shouldEqual 0
    outL.length shouldEqual 3
    outL.head shouldEqual "this a line of print on stderr"
    outL.tail.head shouldEqual "this is a second line"
    outL.tail.tail.head shouldEqual "this is a third line"
  }

  "callPython" should "call the callbacks for stdOut and stdErr when calling the printLinesOutErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    var errL: List[String] = Nil
    val stdErr = (s: String) => {
      errL = errL ++ List(s)
    }
    val (resCode, _) = callPython(Some(stdOut), Some(stdErr), "res.printLinesOutErrAndExit0SubProcess", wd)
    resCode shouldEqual 0
    outL.length shouldEqual 3
    outL.head shouldEqual "this a line of print on stdout"
    outL.tail.head shouldEqual "this is a second line on stdout"
    outL.tail.tail.head shouldEqual "this is a third line on stdout"

    errL.length shouldEqual 3
    errL.head shouldEqual "this a line of print on stderr"
    errL.tail.head shouldEqual "this is a second line on stderr"
    errL.tail.tail.head shouldEqual "this is a third line on stderr"
  }
}
