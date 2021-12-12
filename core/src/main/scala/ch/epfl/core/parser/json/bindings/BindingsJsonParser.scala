package ch.epfl.core.parser.json.bindings

import ch.epfl.core.model.prototypical.AppLibraryBindings
import ch.epfl.core.parser.json.JsonParsingException
import upickle.default.write

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.Using

/** Parser used to store/retrieve AppLibraryBindings through JSON Files
  */
object BindingsJsonParser {

  /** Produce a AppLibraryBindings from a JSON File
    * @param filePath
    * @return
    */
  def parse(filePath: String): AppLibraryBindings = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      parseJson(fileBuff.getLines().mkString)
    }.get
  }

  /** Produces a AppLibraryBindings from a JSON File content
    * @param jsonContent
    * @return
    */
  def parseJson(jsonContent: String): AppLibraryBindings = {
    try {
      upickle.default.read[AppLibraryBindings](jsonContent)
    } catch {
      case e: Exception =>
        throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\n$e")
    }
  }

  /** Write an AppLibraryBindings instance content to a JSON File at the given path
    * @param filePath
    * @param appLibraryBindings
    */
  def writeToFile(filePath: String, appLibraryBindings: AppLibraryBindings): Unit = {
    val f = Paths.get(filePath).toFile
    if (f.exists()) f.delete() // So that we get a fresh copy
    Files.write(Paths.get(filePath), write(appLibraryBindings, indent = 2) getBytes StandardCharsets.UTF_8)
  }

}
