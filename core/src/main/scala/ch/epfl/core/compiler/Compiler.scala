package ch.epfl.core.compiler

import ch.epfl.core.compiler.binding._
import ch.epfl.core.compiler.groupAddressAssigner.GroupAddressAssigner
import ch.epfl.core.compiler.parsers.json.bindings.{BindingsJsonParser, PythonAddressJsonParser}
import ch.epfl.core.compiler.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.physical._
import ch.epfl.core.utils.Constants
import ch.epfl.core.compiler.programming.Programmer

object Compiler {
  def compile(library: ApplicationLibrary, physicalStructure: PhysicalStructure): ApplicationLibrary = {
    //TODO
    // here we need to read assignment of physical communicationObjects to XKNX stuff
    // assign Group addresses to communicationObject
    // generate files for the python apps with the group addresses
    // generate files for the KNX programmming module

    val appLibraryBindings = BindingsJsonParser.parse(library.path + Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME)
    val gaAssignment = GroupAddressAssigner.assignGroupAddressesToPhysical(physicalStructure, appLibraryBindings)
    for (app <- library.apps) {
      val pythonAddr = PythonAddressJsonParser.assignmentToPythonAddressJson(app, gaAssignment)
      PythonAddressJsonParser.writeToFile(app.appFolderPath + Constants.APP_PYTHON_ADDR_BINDINGS_FILE_NAME, pythonAddr)
    }
    Programmer.outputProgrammingFile(gaAssignment)

    library
  }

  def generateBindingsFiles(library: ApplicationLibrary, physicalStructure: PhysicalStructure): Unit = {
    PhysicalStructureJsonParser.writeToFile(library.path + Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME, physicalStructure)
    val appLibraryBindings = Binding.appLibraryBindingsFromLibrary(library)
    BindingsJsonParser.writeToFile(library.path + Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME, appLibraryBindings)
  }

}
