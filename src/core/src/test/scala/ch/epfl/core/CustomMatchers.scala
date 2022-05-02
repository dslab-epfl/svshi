package ch.epfl.core
import ch.epfl.core.model.application.{Application, ApplicationLibrary}
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

  class PathExists() extends Matcher[os.Path] {

    def apply(left: os.Path) = {
      MatchResult(
        os.exists(left),
        s"""Path $left does not exist""",
        s"""Path $left indeed exists"""
      )
    }
  }
  def existInFilesystem() = new PathExists()

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

  class ApplicationLibraryAreSimilarMatcher(other: ApplicationLibrary) extends Matcher[ApplicationLibrary] {

    def apply(left: ApplicationLibrary) = {
      // Compared path from root
      val leftWithSimplifiedPath = ApplicationLibrary(left.apps.map(a => Application(a.name, os.root / a.appFolderPath.segments.toList.last, a.appProtoStructure)), os.root)
      val otherWithSimplifiedPath = ApplicationLibrary(other.apps.map(a => Application(a.name, os.root / a.appFolderPath.segments.toList.last, a.appProtoStructure)), os.root)
      MatchResult(
        leftWithSimplifiedPath == otherWithSimplifiedPath,
        s"""ApplicationLibrary $left is not similar to $other when simplifying Paths as follows:\n$leftWithSimplifiedPath\n$otherWithSimplifiedPath""",
        s"""ApplicationLibrary $left is indeed similar to $other when simplifying Paths as follows:\n$leftWithSimplifiedPath\n$otherWithSimplifiedPath"""
      )
    }
  }
  def beSimilarToLibrary(other: ApplicationLibrary) = new ApplicationLibraryAreSimilarMatcher(other)

  def haveSameContentAsIgnoringBlanks(expectedFile: os.Path) = new FilesHaveSameContentMatcher(other = expectedFile)

  class HttpHeadersContainPair(pair: (String, String)) extends Matcher[Map[String, Seq[String]]] {
    def apply(left: Map[String, Seq[String]]) = {
      MatchResult(
        left.contains(pair._1) && left(pair._1).contains(pair._2),
        s"""Headers $left do not contain $pair. """,
        s"""Headers $left contains $pair"""
      )
    }
  }

  def containThePairOfHeaders(pair: (String, String)) = new HttpHeadersContainPair(pair)

}
// Make them easy to import with:
// import CustomMatchers._
object CustomMatchers extends CustomMatchers
