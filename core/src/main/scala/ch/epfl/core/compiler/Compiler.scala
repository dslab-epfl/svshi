package ch.epfl.core.compiler

import ch.epfl.core.compiler.binding._
import ch.epfl.core.compiler.groupAddressAssigner.GroupAddressAssigner
import ch.epfl.core.compiler.programming.Programmer
import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.models.physical._
import ch.epfl.core.parsers.json.bindings.{BindingsJsonParser, PythonAddressJsonParser}
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants
import ch.epfl.core.utils.Constants.{APP_PROTO_BINDINGS_JSON_FILE_NAME, GENERATED_FOLDER_PATH_STRING, PHYSICAL_STRUCTURE_JSON_FILE_NAME}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object Compiler {
  def compile(newAppsLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, physicalStructure: PhysicalStructure): (ApplicationLibrary, ApplicationLibrary, GroupAddressAssignment) = {
    val appLibraryBindings = BindingsJsonParser.parse(Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(APP_PROTO_BINDINGS_JSON_FILE_NAME)).toString)
    val gaAssignment = GroupAddressAssigner.assignGroupAddressesToPhysical(physicalStructure, appLibraryBindings)

    val filePath: Path = (os.pwd / os.up / Constants.GENERATED_FOLDER_NAME / Constants.GROUP_ADDRESSES_LIST_FILE_NAME).toNIO

    generateGroupAddressesList(gaAssignment, filePath)

    for (app <- existingAppsLibrary.apps.appendedAll(newAppsLibrary.apps)) {
      val pythonAddr = PythonAddressJsonParser.assignmentToPythonAddressJson(app, gaAssignment)
      PythonAddressJsonParser.writeToFile(Path.of(app.appFolderPath).resolve(Path.of(Constants.APP_PYTHON_ADDR_BINDINGS_FILE_NAME)).toString, pythonAddr)
    }
    Programmer.outputProgrammingFile(gaAssignment)

    (newAppsLibrary, existingAppsLibrary, gaAssignment)
  }

  def generateBindingsFiles(newAppsLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, newPhysStruct: PhysicalStructure, existingPhysStruct: PhysicalStructure): Unit = {
    PhysicalStructureJsonParser.writeToFile(
      Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(PHYSICAL_STRUCTURE_JSON_FILE_NAME)).toString,
      newPhysStruct
    )
    val appLibraryBindings = Binding.appLibraryBindingsFromLibrary(newAppsLibrary, existingAppsLibrary, newPhysStruct, existingPhysStruct)
    BindingsJsonParser.writeToFile(Path.of(GENERATED_FOLDER_PATH_STRING).resolve(Path.of(APP_PROTO_BINDINGS_JSON_FILE_NAME)).toString, appLibraryBindings)
  }

  def generateGroupAddressesList(groupAddressAssignment: GroupAddressAssignment, filePath: Path): Unit = {
    val list = groupAddressAssignment.getPythonTypesMap.toList.map{case (groupAddr, pythonTypesList) => (groupAddr.toString, pythonTypesList.map(_.toString).min)}
    val groupAddresses = GroupAddressesList(list)
    val json = upickle.default.write(groupAddresses)
    val file = filePath.toFile
    if (file.exists()) file.delete()
    Files.write(filePath, json getBytes StandardCharsets.UTF_8)
  }

}
