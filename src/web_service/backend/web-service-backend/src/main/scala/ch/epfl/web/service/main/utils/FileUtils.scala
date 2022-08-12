package ch.epfl.web.service.main.utils

import java.io.ByteArrayOutputStream
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Using}

/** Utility functions to manipulate files in the file system
  */
object FileUtils {

  private val FILES_OR_DIRS_NAMES_TO_REMOVE = List("__MACOSX")

  /** The suffix appended to the end of a ETS project file name when unzipped
    * @return
    */
  def unzippedSuffix: String = "_unzip_temp"

  /** Unzip the archive at the given path.
    * If the given path exists, it is first emptied
    * @param zipPath
    * @return outputPath
    */
  def unzip(zipPath: os.Path, outputFolderPath: os.Path): Option[os.Path] = {
    Using(new ZipFile(zipPath.toIO)) { zipFile =>
      if (os.exists(outputFolderPath)) os.remove.all(outputFolderPath)
      os.makeDir.all(outputFolderPath) // In case the zip is empty, the folder is created nonetheless
      for (entry <- zipFile.entries.asScala) {
        if (!FILES_OR_DIRS_NAMES_TO_REMOVE.exists(n => entry.getName.contains(n))) {
          val path = os.Path(entry.getName, outputFolderPath)
          if (entry.isDirectory) {
            os.makeDir.all(path)
          } else {
            val parentPath = path / os.up
            os.makeDir.all(parentPath)
            os.write(path, zipFile.getInputStream(entry))
          }
        }
      }
      Some(outputFolderPath)
    }.getOrElse(None)
  }

  /** Create a new Archive in the file given by outputZip containing the files and or directories in toZip.
    * A directory in the toZip list is added to the zip archive with all its children directory and files
    * If the outputZip file already exists, it is replaced.
    * The function creates parents of the file if they do not exist
    * If the outputZip is a directory, a new IllegalArgument exception is thrown
    * @param toZip: the directory or file to zip
    * @param outputZip
    */
  def zip(toZip: List[os.Path], outputZip: os.Path): Option[os.Path] = {
    if (os.isDir(outputZip)) throw new IllegalArgumentException("The outputZip must not be a directory!")
    toZip.foreach(p => if (!os.exists(p)) throw new IllegalArgumentException(f"All files and directories in toZip list must exist! $p does not exit!"))

    // Create parents directories of the output file
    os.makeDir.all(outputZip / os.up)

    zipInMem(toZip) match {
      case Some(value) => {
        os.write.over(outputZip, value)
        Some(outputZip)
      }
      case None => None
    }

  }

  /** Create an in-memory zip archive with the given file(s) compressed. Return the zip file as an Array of Byte
    * @param file
    * @return
    */
  def zipInMem(toZip: List[os.Path]): Option[Array[Byte]] = {
    def addFileToZip(root: os.Path, file: os.Path, zipOutputStream: ZipOutputStream): Unit = {
      assert(os.isFile(file))
      val entryName = file.relativeTo(root).toString()
      val entry = new ZipEntry(entryName)
      zipOutputStream.putNextEntry(entry)

      val fis = os.read.inputStream(file)
      val bytes = new Array[Byte](1024)
      var length = fis.read(bytes)
      while (length >= 0) {
        zipOutputStream.write(bytes, 0, length)
        length = fis.read(bytes)
      }
      fis.close()
    }

    def addDirToZip(dir: os.Path, zipOutputStream: ZipOutputStream): Unit = {
      assert(os.isDir(dir))
      val files = FileUtils.recursiveListFiles(dir)
      files.foreach(f => if (os.isFile(f)) addFileToZip(dir / os.up, f, zipOutputStream))
    }
    val outputStream = new ByteArrayOutputStream()

    val res = Using(new ZipOutputStream(outputStream)) { zipOs =>
      toZip.foreach(p => {
        if (os.isDir(p)) addDirToZip(p, zipOs) else addFileToZip(p / os.up, p, zipOs)
      })
    } match {
      case Failure(_) => None
      case Success(_) => Some(outputStream.toByteArray)
    }
    outputStream.close()
    res

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
    * @return the list of Paths of directories
    */
  def getListOfFolders(dir: os.Path): List[os.Path] = {
    if (os.exists(dir) && os.isDir(dir)) {
      os.list(dir).filter(a => os.isDir(a)).toList
    } else {
      Nil
    }
  }

  /** Explore the given directory and output the list of files contained in this directory
    * @param dir the path to the directory to explore
    * @return the list of Paths of files
    */
  def getListOfFiles(dir: os.Path): List[os.Path] = {
    if (os.exists(dir) && os.isDir(dir)) {
      os.list(dir).filter(a => os.isFile(a)).toList
    } else {
      Nil
    }
  }

  /** Move all the files and directories contained in dir into destinationDir.
    * If a file already exist in the destination folder, it is replaced and folders are merged
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
    if (!os.isFile(filePath)) throw new IllegalArgumentException(s"filePath $filePath must be a file!")
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
    if (!os.isFile(filePath)) throw new IllegalArgumentException(s"filePath $filePath must be a file!")
    if (!os.isDir(destinationDir)) throw new IllegalArgumentException("destinationDir must be a path to a directory!")

    os.copy(filePath, destinationDir / newFileName, replaceExisting = true)
  }

  /** Copy the directory and its content into destinationDir while renaming it. If the destinationDir does not exist, it creates it
    * WARNING! if a directory with the new name already exists in the destination directory, it is replaced
    *
    * @param dirPath the file to copy
    * @param destinationDir the directory in which the file is copied
    * @param newDirName the new name the file must have
    */
  def copyDirWithNewName(dirPath: os.Path, destinationDir: os.Path, newDirName: String): Unit = {
    if (!os.exists(dirPath)) throw new IllegalArgumentException("dirPath must be an existing path!")
    if (!os.isDir(dirPath)) throw new IllegalArgumentException(s"dirPath $dirPath must be a directory!")
    if (os.exists(destinationDir) && os.isFile(destinationDir)) throw new IllegalArgumentException("destinationDir must not be a file!")

    val newDirPath = destinationDir / newDirName
    if (os.exists(newDirPath)) os.remove.all(newDirPath)
    os.makeDir.all(newDirPath)
    os.copy(dirPath, newDirPath, mergeFolders = true, replaceExisting = true)
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
    os.Path(pathString, os.Path(Constants.SVSHI_WEBSERVICE_HOME_STR, os.pwd))
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

  /** Write the content to the file pointed by the path, creating parent folder(s) if needed
    * If the file already exists, this function overwrite its content
    * @param path
    * @param data
    */
  def writeToFileOverwrite(path: os.Path, data: Array[Byte]): Unit = {
    os.makeDir.all(path / os.up)
    os.write.over(path, data)
  }

  /** Write the content to the file pointed by the path, creating parent folder(s) if needed
    * If the file already exists, this function append the new content at the end
    * @param path
    * @param data
    */
  def writeToFileAppend(path: os.Path, data: Array[Byte]): Unit = {
    os.makeDir.all(path / os.up)
    os.write.append(path, data)
  }

  /** Return the extension of the given file as a String.
    * If the given path points to a directory, it returns the empty String.
    * If the given path does not exist, it returns the empty String.
    * If the filename is only an extension (e.g., `.gitignore`), it returns the extension.
    * If the file has no extension, it returns the empty string.
    * @param file the path to the file
    * @return the extension as a string
    */
  def getFileExtension(file: os.Path): String = {
    if (os.isFile(file)) {
      val filename = file.segments.toList.last
      if (filename.contains(".")) filename.split('.').toList.last else ""
    } else {
      ""
    }
  }

}
