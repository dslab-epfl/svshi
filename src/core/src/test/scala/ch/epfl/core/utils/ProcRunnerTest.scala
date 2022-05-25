package ch.epfl.core.utils

import ch.epfl.core.utils.ProcRunner.{callPythonBlocking, callPythonNonBlocking}
import ch.epfl.core.utils.subprocess.SvshiSubProcess
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Futures.timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.language.postfixOps

class ProcRunnerTest extends AnyFlatSpec with Matchers with BeforeAndAfterEach {
  private var currentSubProcess: Option[SvshiSubProcess] = None
  "callPythonBlocking" should "return the exit code 1 and the two lines of text when calling the failingPythonSubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val (resCode, output) = callPythonBlocking(None, None, "res.failingPythonSubprocess", wd)
    resCode shouldEqual 1
    output.length shouldEqual 2
    output.head shouldEqual "stdout: this a line of error"
    output.tail.head shouldEqual "stdout: this is a second line"
  }
  "callPythonBlocking" should "call the callbacks for stdOut when calling the printLinesOutAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    val (resCode, _) = callPythonBlocking(Some(stdOut), None, "res.printLinesOutAndExit0SubProcess", wd)
    resCode shouldEqual 0
    outL.length shouldEqual 3
    outL.head shouldEqual "this a line of print on stdout"
    outL.tail.head shouldEqual "this is a second line"
    outL.tail.tail.head shouldEqual "this is a third line"
  }

  "callPythonBlocking" should "call the callbacks for stdErr when calling the printLinesErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdErr = (s: String) => {
      outL = outL ++ List(s)
    }
    val (resCode, _) = callPythonBlocking(None, Some(stdErr), "res.printLinesErrAndExit0SubProcess", wd)
    resCode shouldEqual 0
    outL.length shouldEqual 3
    outL.head shouldEqual "this a line of print on stderr"
    outL.tail.head shouldEqual "this is a second line"
    outL.tail.tail.head shouldEqual "this is a third line"
  }

  "callPythonBlocking" should "call the callbacks for stdOut and stdErr when calling the printLinesOutErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    var errL: List[String] = Nil
    val stdErr = (s: String) => {
      errL = errL ++ List(s)
    }
    val (resCode, _) = callPythonBlocking(Some(stdOut), Some(stdErr), "res.printLinesOutErrAndExit0SubProcess", wd)
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

  "callPythonNonBlocking" should "return a subprocess that return the correct exit code Option" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val svshiSubProcess = ProcRunner.callPythonNonBlocking(None, None, "res.wait3secsThenExits0SubProcess", wd)

    svshiSubProcess.exitCode shouldEqual None

    eventually(timeout(10 seconds)) {
      svshiSubProcess.exitCode shouldEqual Some(0)
    }
  }

  "callPythonNonBlocking" should "return a subprocess that can be correctly killed" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val svshiSubProcess = ProcRunner.callPythonNonBlocking(None, None, "res.wait3secsThenExits0SubProcess", wd)

    svshiSubProcess.exitCode shouldEqual None
    svshiSubProcess.forceStop()

    eventually(timeout(1 seconds)) {
      svshiSubProcess.exitCode.isDefined shouldBe true
      svshiSubProcess.exitCode.get shouldNot equal(0)
    }
  }

  "callPythonNonBlocking" should "return a subprocess that can offers working isAlive function" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val svshiSubProcess = ProcRunner.callPythonNonBlocking(None, None, "res.wait3secsThenExits0SubProcess", wd)

    svshiSubProcess.isAlive shouldBe true

    eventually(timeout(5 seconds)) {
      svshiSubProcess.isAlive shouldBe false
    }
  }

  "callPythonNonBlocking" should "return a subprocess that can offers working isAlive function when killed" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    val svshiSubProcess = ProcRunner.callPythonNonBlocking(None, None, "res.wait3secsThenExits0SubProcess", wd)

    svshiSubProcess.isAlive shouldBe true
    svshiSubProcess.forceStop()

    eventually(timeout(1 seconds)) {
      svshiSubProcess.isAlive shouldBe false
    }
  }

  "callPythonNonBlocking" should "call the callbacks for stdOut and stdErr when calling the printLinesOutErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    var errL: List[String] = Nil
    val stdErr = (s: String) => {
      errL = errL ++ List(s)
    }
    callPythonNonBlocking(Some(stdOut), Some(stdErr), "res.printLinesOutErrAndExit0SubProcess", wd)
    eventually(timeout(5 seconds)) {
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

  "callPythonNonBlocking" should "call the callbacks for stdOut when calling the printLinesOutAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdOut = (s: String) => {
      outL = outL ++ List(s)
    }
    callPythonNonBlocking(Some(stdOut), None, "res.printLinesOutAndExit0SubProcess", wd)
    eventually(timeout(5 seconds)) {
      outL.length shouldEqual 3
      outL.head shouldEqual "this a line of print on stdout"
      outL.tail.head shouldEqual "this is a second line"
      outL.tail.tail.head shouldEqual "this is a third line"
    }
  }

  "callPythonNonBlocking" should "call the callbacks for stdErr when calling the printLinesErrAndExit0SubProcess" in {
    val wd = Constants.SVSHI_SRC_FOLDER_PATH / "core"
    var outL: List[String] = Nil
    val stdErr = (s: String) => {
      outL = outL ++ List(s)
    }
    callPythonNonBlocking(None, Some(stdErr), "res.printLinesErrAndExit0SubProcess", wd)
    eventually(timeout(5 seconds)) {
      outL.length shouldEqual 3
      outL.head shouldEqual "this a line of print on stderr"
      outL.tail.head shouldEqual "this is a second line"
      outL.tail.tail.head shouldEqual "this is a third line"
    }
  }
}
