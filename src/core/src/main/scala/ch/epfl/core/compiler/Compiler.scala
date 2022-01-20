package ch.epfl.core.compiler

import ch.epfl.core.compiler.bindings._
import ch.epfl.core.compiler.bindings.groupAddressAssigner.GroupAddressAssigner
import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical.AppLibraryBindings
import ch.epfl.core.parser.json.bindings.{BindingsJsonParser, PythonAddressJsonParser}
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.utils.Constants
import ch.epfl.core.utils.Constants.{APP_PROTO_BINDINGS_JSON_FILE_NAME, GENERATED_FOLDER_PATH, PHYSICAL_STRUCTURE_JSON_FILE_NAME}

import java.nio.charset.StandardCharsets
import java.nio.file.Files

object Compiler {

  /** Compile applications given the applications to be added, the existing ones and the physical structure
    * @param newAppsLibrary
    * @param existingAppsLibrary
    * @param physicalStructure
    * @return a tuple containing the (newApplicationsLibrary, existingApplicationsLibrary, groupAddressAssignment) produced.
    *         Libraries are unchanged from the inputs
    */
  def compile(
      newAppsLibrary: ApplicationLibrary,
      existingAppsLibrary: ApplicationLibrary,
      physicalStructure: PhysicalStructure
  ): (ApplicationLibrary, ApplicationLibrary, GroupAddressAssignment) = {
    val appLibraryBindings = BindingsJsonParser.parse(GENERATED_FOLDER_PATH / APP_PROTO_BINDINGS_JSON_FILE_NAME)

    // Check that the bindings received are compatible (equivalent modulo the physical IDs) to a freshly generated copy
    val freshBindings = Bindings.appLibraryBindingsFromLibrary(newAppsLibrary, existingAppsLibrary, physicalStructure, physicalStructure)
    if (!appLibraryBindings.equivalent(freshBindings)) throw IncompatibleBindingsException()

    // Filter to make sure that there are no stale bindings that relate to applications not installed anymore
    val filteredAppLibraryBindings = AppLibraryBindings(appLibraryBindings.appBindings.filter(b => (newAppsLibrary.apps ++ existingAppsLibrary.apps).exists(a => a.name == b.name)))
    val gaAssignment = GroupAddressAssigner.assignGroupAddressesToPhysical(physicalStructure, filteredAppLibraryBindings)

    val filePath: os.Path = Constants.GENERATED_FOLDER_PATH / Constants.GROUP_ADDRESSES_LIST_FILE_NAME

    generateGroupAddressesList(gaAssignment, filePath)

    for (app <- existingAppsLibrary.apps.appendedAll(newAppsLibrary.apps)) {
      val pythonAddr = PythonAddressJsonParser.assignmentToPythonAddressJson(app, gaAssignment)
      PythonAddressJsonParser.writeToFile(app.appFolderPath / Constants.APP_PYTHON_ADDR_BINDINGS_FILE_NAME, pythonAddr)
    }
    (newAppsLibrary, existingAppsLibrary, gaAssignment)
  }

  /** Generate bindings files for the two passed libraries using physical structure.
    * If the physical structure didn't change (i.e., new and existing physical structures are identical), the bindings from
    * the existing libraries are reused and added to the new bindings (set to default values).
    * @param newAppsLibrary
    * @param existingAppsLibrary
    * @param newPhysStruct
    * @param existingPhysStruct
    */
  def generateBindingsFiles(
      newAppsLibrary: ApplicationLibrary,
      existingAppsLibrary: ApplicationLibrary,
      newPhysStruct: PhysicalStructure,
      existingPhysStruct: PhysicalStructure
  ): Unit = {
    PhysicalStructureJsonParser.writeToFile(
      GENERATED_FOLDER_PATH / PHYSICAL_STRUCTURE_JSON_FILE_NAME,
      newPhysStruct
    )
    val appLibraryBindings = Bindings.appLibraryBindingsFromLibrary(newAppsLibrary, existingAppsLibrary, newPhysStruct, existingPhysStruct)
    BindingsJsonParser.writeToFile(GENERATED_FOLDER_PATH / APP_PROTO_BINDINGS_JSON_FILE_NAME, appLibraryBindings)
  }

  /** Write the JSON file containing mapping between used group addresses and their corresponding datatype in Python
    * @param groupAddressAssignment
    * @param filePath
    */
  def generateGroupAddressesList(groupAddressAssignment: GroupAddressAssignment, filePath: os.Path): Unit = {
    val pythonTypesMap = groupAddressAssignment.getPythonTypesMap
    val dptMap = groupAddressAssignment.getDPTsMap
    val pythonList = pythonTypesMap.toList.map { case (groupAddr, pythonTypesList) => (groupAddr, pythonTypesList.map(_.toString).min) }

    val list = pythonList.map { case (ga, pythonType) => (ga.toString, pythonType, dptMap(ga).map(_.toString).min) }
    val groupAddresses = GroupAddressesList(list)
    val json = upickle.default.write(groupAddresses)
    val filePathNio = filePath.toNIO
    val file = filePathNio.toFile
    if (file.exists()) file.delete()
    Files.write(filePathNio, json getBytes StandardCharsets.UTF_8)
  }

}

case class IncompatibleBindingsException() extends Exception
