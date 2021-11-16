package ch.epfl.core.compiler.parsers.json.bindings

import ch.epfl.core.compiler.parsers.json.JsonParsingException
import ch.epfl.core.models.prototypical.AppLibraryBindings
import upickle.default.write

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.Using

object BindingsJsonParser {
  def parse(filePath: String): AppLibraryBindings = {
    Using(Source.fromFile(filePath)) { fileBuff =>
      parseJson(fileBuff.getLines().mkString)
    }.get
  }


  def parseJson(jsonContent: String): AppLibraryBindings = {
    try {
      upickle.default.read[AppLibraryBindings](jsonContent)
    } catch {
      case e: Exception =>
        throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\n$e")
    }
  }

  def writeToFile(filePath: String, appLibraryBindings: AppLibraryBindings): Unit = {
    val f = Paths.get(filePath).toFile
    if (f.exists()) f.delete() // So that we get a fresh copy
    Files.write(Paths.get(filePath), write(appLibraryBindings, indent = 2) getBytes StandardCharsets.UTF_8)
  }


}
