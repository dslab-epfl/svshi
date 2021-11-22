package ch.epfl.core.compiler

import ch.epfl.core.compiler.binding._
import ch.epfl.core.compiler.groupAddressAssigner.GroupAddressAssigner
import ch.epfl.core.parsers.json.bindings.{BindingsJsonParser, PythonAddressJsonParser}
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.physical._
import ch.epfl.core.utils.Constants
import ch.epfl.core.compiler.programming.Programmer
import ch.epfl.core.utils.Constants.{APP_PROTO_BINDINGS_JSON_FILE_NAME, GENERATED_FOLDER_PATH_STRING, PHYSICAL_STRUCTURE_JSON_FILE_NAME}

import java.nio.file.Path

object Compiler {
  def compile(newAppsLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, physicalStructure: PhysicalStructure): (ApplicationLibrary, ApplicationLibrary) = {
    //TODO
    // here we need to read assignment of physical communicationObjects to XKNX stuff
    // assign Group addresses to communicationObject
    // generate files for the python apps with the group addresses
    // generate files for the KNX programmming module

    val appLibraryBindings = BindingsJsonParser.parse(Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(APP_PROTO_BINDINGS_JSON_FILE_NAME)).toString)
    val gaAssignment = GroupAddressAssigner.assignGroupAddressesToPhysical(physicalStructure, appLibraryBindings)

    generateGroupAddressesList(gaAssignment.physIdToGA.values)

    for (app <- existingAppsLibrary.apps.appendedAll(newAppsLibrary.apps)) {
      val pythonAddr = PythonAddressJsonParser.assignmentToPythonAddressJson(app, gaAssignment)
      PythonAddressJsonParser.writeToFile(Path.of(app.appFolderPath).resolve(Path.of(Constants.APP_PYTHON_ADDR_BINDINGS_FILE_NAME)).toString, pythonAddr)
    }
    Programmer.outputProgrammingFile(gaAssignment)

    // TODO move new app in the existing library
    (newAppsLibrary, existingAppsLibrary)
  }

  def generateBindingsFiles(newAppsLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, physicalStructure: PhysicalStructure): Unit = {
    PhysicalStructureJsonParser.writeToFile(
      Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(PHYSICAL_STRUCTURE_JSON_FILE_NAME)).toString,
      physicalStructure
    )
    val appLibraryBindings = Binding.appLibraryBindingsFromLibrary(newAppsLibrary, existingAppsLibrary)
    BindingsJsonParser.writeToFile(Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(APP_PROTO_BINDINGS_JSON_FILE_NAME)).toString, appLibraryBindings)
  }

  private def generateGroupAddressesList(groupAddresses: Iterable[GroupAddress]) = {
    val list = GroupAddressesList(groupAddresses.map(_.toString()).toList)
    val json = upickle.default.write(list)
    val filePath = os.pwd / os.up / Constants.GENERATED_FOLDER_NAME / Constants.GROUP_ADDRESSES_LIST_FILE_NAME
    if (os.exists(filePath)) os.remove(filePath)
    os.write(filePath, json)
  }

}
