package ch.epfl.core
import org.scalatest.matchers._
import os.Path

trait CustomMatchers {

  class PathIsFile() extends Matcher[os.Path] {

    def apply(left: os.Path) = {
      MatchResult(
        os.isFile(left),
        s"""Path $left did not point to a file""",
        s"""Path $left indeed was a file"""
      )
    }
  }
  def beAFile() = new PathIsFile()

  class FilesHaveSameContentMatcher(other: Path) extends Matcher[os.Path] {

    def apply(left: os.Path) = {
      val content1 = os.read(left)
      val content2 = os.read(other)
      MatchResult(
        sameContentIgnoreBlanks(left, other),
        s"""File $left did not have same content as $other.\nContent was:\n$content1\n but expected was:\n$content2 """,
        s"""File $left had same content as $other"""
      )
    }
    private def sameContentIgnoreBlanks(file1: os.Path, file2: os.Path): Boolean = {
      val content1 = os.read.lines(file1).filterNot(_.isBlank)
      val content2 = os.read.lines(file2).filterNot(_.isBlank)

      for ((l, count) <- content1.zipWithIndex) {
        if (l != content2(count)) {
          return false
        }
      }
      return true
    }
  }

  def haveSameContentAsIgnoringBlanks(expectedFile: os.Path) = new FilesHaveSameContentMatcher(other = expectedFile)
}

// Make them easy to import with:
// import CustomMatchers._
object CustomMatchers extends CustomMatchers
