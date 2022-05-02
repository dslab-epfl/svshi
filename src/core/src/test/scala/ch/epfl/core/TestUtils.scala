package ch.epfl.core

import ch.epfl.core.CustomMatchers.{beAFile, haveSameContentAsIgnoringBlanks}
import ch.epfl.core.utils.FileUtils
import org.scalatest.matchers.should.Matchers
import os.Path

object TestUtils extends Matchers {

  // Files that should be ignored when comparing folders
  val defaultIgnoredFilesAndDir = List(".DS_Store", ".gitkeep", "__MACOSX")

  // Compare the two folder and assert that they contain the same files and that files are identical
  def compareFolders(folder1: Path, folder2: Path, ignoredFileAndDirNames: List[String]): Unit = {
    os.isDir(folder1) shouldBe true
    os.isDir(folder2) shouldBe true

    // Compare content of the folder
    val toRemoveF1 = FileUtils.recursiveListFiles(folder1).filter(p => ignoredFileAndDirNames.contains(p.segments.toList.last))
    val toRemoveF2 = FileUtils.recursiveListFiles(folder2).filter(p => ignoredFileAndDirNames.contains(p.segments.toList.last))
    val folder1Content = FileUtils.recursiveListFiles(folder1).filterNot(p => toRemoveF1.exists(pp => p.startsWith(pp)))
    val folder2Content = FileUtils.recursiveListFiles(folder2).filterNot(p => toRemoveF2.exists(pp => p.startsWith(pp)))

    for (e <- folder1Content) {
      folder2Content.map(f => f.relativeTo(folder2)) should contain(e.relativeTo(folder1))
    }
    for (e <- folder2Content) {
      folder1Content.map(f => f.relativeTo(folder1)) should contain(e.relativeTo(folder2))
    }

    for (f <- folder1Content) {
      if (os.isFile(f)) {
        val fRel = f.relativeTo(folder1)
        val fIn2 = os.Path(fRel, folder2)
        fIn2 should beAFile()
        f should haveSameContentAsIgnoringBlanks(fIn2)
      }
    }
  }
}
