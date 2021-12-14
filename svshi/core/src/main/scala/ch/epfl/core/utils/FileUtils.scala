package ch.epfl.core.utils

import java.io.File
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters._
import scala.util.Using

/** Utility functions to manipulate files in the file system
  */
object FileUtils {

  /** The suffix appended to the end of a ETS project file name when unzipped
    * @return
    */
  def unzippedSuffix: String = "_unzip_temp"

  /** Unzip the archive at the given path
    * @param zipPath
    * @return outputPath
    */
  def unzip(zipPath: os.Path, outputFolderPath: os.Path): Option[os.Path] = {
    Using(new ZipFile(zipPath.toIO)) { zipFile =>
      for (entry <- zipFile.entries.asScala) {
        val path = os.Path(entry.getName, outputFolderPath)
        if (entry.isDirectory) {
          os.makeDir.all(path)
        } else {
          val parentPath = os.Path("/" + path.segments.toList.reverse.tail.reverse.mkString("/"))
          os.makeDir.all(parentPath)
          os.write(path, zipFile.getInputStream(entry))
        }
      }
      Some(outputFolderPath)
    }.getOrElse(None)
  }

  /** List all files in the given directory recursively
    * @param f a directory
    * @return
    */
  def recursiveListFiles(f: os.Path): List[os.Path] = {
    if(os.isDir(f)){
      val these = os.list(f).toList
      these ++ these.flatMap(recursiveListFiles)
    } else {
      Nil
    }


  }

  /** explore the given directory and outputs the list of folders contained in this directory
    * @param dir the path to the directory to explore
    * @return
    */
  def getListOfFolders(dir: os.Path): List[os.Path] = {
    if (os.exists(dir) && os.isDir(dir)) {
      os.list(dir).filter(a => os.isDir(a)).toList
    } else {
      Nil
    }
  }

  /** Move all the files and directories contained in dir into destinationDir
    * @param dir parent directory that contains all files and directories that have to be moved
    * @param destinationDir the directory in which all files and directories are moved
    */
  def moveAllFileToOtherDirectory(dir: String, destinationDir: String): Unit = {
    val from = new File(dir)
    if (!from.isDirectory) throw new IllegalArgumentException("dir must be a path to a directory!")
    val to = new File(dir)
    if (!to.isDirectory) throw new IllegalArgumentException("destinationDir must be a path to a directory!")
    val fromOsPath = os.Path(dir, base = os.pwd)
    val toOsPath = os.Path(destinationDir, base = os.pwd)
    os.copy(fromOsPath, toOsPath, mergeFolders = true, replaceExisting = true)
    os.remove.all(fromOsPath)
    os.makeDir(fromOsPath)
  }

  def getPathFromSvshiHome(pathString: String): os.Path = {
    os.Path(pathString, os.Path(Constants.SVSHI_HOME, os.pwd))
  }

}
