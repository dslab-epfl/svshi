package ch.epfl.core.api.server

import ch.epfl.core.utils.FileUtils
import os.Path

object Logger {
  val MAX_FILE_SIZE_B: Long = 20 * 1024 * 1024 // == 20MB
  def log(file: Path, prefix: String, message: String): Unit = {
    val toWrite = if (prefix == "") f"$message\n" else f"$prefix: $message\n"
    FileUtils.writeToFileAppend(file, toWrite.getBytes)
    FileUtils.checkSizeAndReduce(file, MAX_FILE_SIZE_B)
  }
}
