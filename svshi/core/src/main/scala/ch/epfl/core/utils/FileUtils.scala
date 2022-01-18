package ch.epfl.core.utils

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
          val parentPath = path / os.up
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
    if (os.isDir(f)) {
      val these = os.list(f).toList
      these ++ these.flatMap(recursiveListFiles)
    } else {
      Nil
    }

  }

  /** Explore the given directory and output the list of folders contained in this directory
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
  def moveAllFileToOtherDirectory(dir: os.Path, destinationDir: os.Path): Unit = {
    if (!os.isDir(dir)) throw new IllegalArgumentException("dir must be a path to a directory!")
    if (!os.isDir(destinationDir)) throw new IllegalArgumentException("destinationDir must be a path to a directory!")
    os.copy(dir, destinationDir, mergeFolders = true, replaceExisting = true)
    os.remove.all(dir)
    os.makeDir(dir)
  }

  /** Copy the file into destinationDir
    *
    * @param filePath the file to copy
    * @param destinationDir the directory in which the file is copied
    */
  def copyFile(filePath: os.Path, destinationDir: os.Path): Unit = {
    if (!os.exists(filePath)) throw new IllegalArgumentException(s"filePath $filePath must be an existing path!")
    if (!os.isDir(destinationDir)) throw new IllegalArgumentException(s"destinationDir $destinationDir must be a path to a directory!")

    os.copy.into(filePath, destinationDir, replaceExisting = true)
  }

  /** Copy the file into destinationDir while renaming it
    * WARNING! if a file with the new name already exists in the destination directory, it is replaced
    *
    * @param filePath the file to copy
    * @param destinationDir the directory in which the file is copied
    * @param newFileName the new name the file must have
    */
  def copyFileWithNewName(filePath: os.Path, destinationDir: os.Path, newFileName: String): Unit = {
    if (!os.exists(filePath)) throw new IllegalArgumentException("filePath must be an existing path!")
    if (!os.isDir(destinationDir)) throw new IllegalArgumentException("destinationDir must be a path to a directory!")

    os.copy(filePath, destinationDir / newFileName, replaceExisting = true)
  }

  /** Copies the files into destinationDir
    *
    * @param filePaths the files to copy
    * @param destinationDir the directory in which the files are copied
    */
  def copyFiles(filePaths: List[os.Path], destinationDir: os.Path): Unit = {
    if (!os.isDir(destinationDir)) throw new IllegalArgumentException("destinationDir must be a path to a directory!")

    filePaths.foreach(path => copyFile(path, destinationDir))
  }

  /** Return a os.Path instance from the pathString passed that must be relation to $SVSHI_HOME
    * @param pathString
    * @return
    */
  def getPathFromSvshiHome(pathString: String): os.Path = {
    os.Path(pathString, os.Path(Constants.SVSHI_HOME, os.pwd))
  }

  /** Read the file and returns the content
    */
  def readFileContentAsString(path: os.Path): String = {
    os.read(path)
  }

  /** Remove the file pointed by the path if it exists
    * @param path
    */
  def deleteIfExists(path: os.Path): Unit = {
    if (os.exists(path)) os.remove.all(path)
  }

  /** Write the content to the file pointed by the path
    * @param path
    * @param data
    */
  def writeToFile(path: os.Path, data: Array[Byte]): Unit = {

    os.write(path, data)
  }

}
