package ch.epfl.core.utils

import java.io.File
import java.nio.file.{Files, Path}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters._
import scala.util.Using

object FileUtils {

  /** The suffix append to the end of a ETS project file name when unzipped
    * @return
    */
  def unzippedSuffix: String = "_unzip_temp"

  /** Unzip the archive at the given path
    * @param zipPathString
    * @return outputPath
    */
  def unzip(
      zipPathString: String,
      outputFolderPathString: String
  ): Option[Path] = {
    Using(new ZipFile(Path.of(zipPathString).toFile)) { zipFile =>
      for (entry <- zipFile.entries.asScala) {
        val path = Path.of(outputFolderPathString).resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(path)
        } else {
          Files.createDirectories(path.getParent)
          Files.copy(zipFile.getInputStream(entry), path)
        }
      }
      Some(Path.of(outputFolderPathString))
    }.getOrElse(None)
  }

  /** List all files in the given directory recursively
    * @param f a directory
    * @return
    */
  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

}
