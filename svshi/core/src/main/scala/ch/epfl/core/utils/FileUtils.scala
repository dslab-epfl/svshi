package ch.epfl.core.utils

import java.io.File
import java.nio.file.Files
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
    Using(new ZipFile(zipPath.toNIO.toFile)) { zipFile =>
      for (entry <- zipFile.entries.asScala) {
        val path = outputFolderPath.toNIO.resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(path)
        } else {
          Files.createDirectories(path.getParent)
          Files.copy(zipFile.getInputStream(entry), path)
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
  def getListOfFolders(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isDirectory).toList
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
