package ch.epfl.core.compiler.knxProgramming

import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.utils.Constants.ASSIGNMENTS_DIRECTORY_PATH
import ch.epfl.core.utils.FileUtils
import com.github.tototoshi.csv.CSVWriter
import upickle.default.write

/** KNX programmer
  *
  * @param assignment the group address assignment data structure
  */
case class Programmer(assignment: GroupAddressAssignment) {

  private case class Assignment(deviceName: String, deviceAddress: String, nodeName: String, commObjectName: String, groupAddress: GroupAddress, datatype: KNXDatatype)

  private val assignments = {
    val idToGroupAddress = assignment.physIdToGA
    val dpts = assignment.getDPTsMap
    assignment.physStruct.deviceInstances.flatMap { case PhysicalDevice(deviceName, (a1, a2, a3), nodes) =>
      val addressString = s"$a1.$a2.$a3"
      nodes.flatMap { case PhysicalDeviceNode(nodeName, comObjects) =>
        comObjects.filter(idToGroupAddress contains _.id).map { case PhysicalDeviceCommObject(objName, datatype, ioType, id) =>
          val groupAddress = idToGroupAddress(id)
          val dtype = dpts(groupAddress).head
          Assignment(deviceName, addressString, nodeName, objName, groupAddress, dtype)
        }
      }
    }
  }

  private val CSV_HEADER = List("Main", "Middle", "Sub", "Address", "Central", "Unfiltered", "Description", "DatapointType", "Security")
  private val CSV_MAIN_GROUP = List("New main group", "", "", "0/-/-", "", "", "", "", "Auto")
  private val CSV_MIDDLE_GROUP = List("", "New middle group", "", "0/0/-", "", "", "", "", "Auto")

  /** Outputs the programming file with the group address assignments for each communication object.
    *
    * @param filename the output filename. Defaults to "assignment.txt"
    */
  def outputProgrammingFile(filename: String = "assignment.txt"): Unit = {
    val tree = assignments.groupBy(_.deviceAddress).map { case (deviceAddress, assignments) =>
      s"device address '$deviceAddress'\n" + assignments
        .groupBy(_.nodeName)
        .map { case (nodeName, assigns) =>
          s"  node '$nodeName'\n" + assigns.map(a => s"    comm object '${a.commObjectName}' => group address '${a.groupAddress}'").mkString("\n")
        }
        .mkString("\n")
    }
    val text = tree.mkString("\n")
    val directoryPath = ASSIGNMENTS_DIRECTORY_PATH
    if (!os.exists(directoryPath)) os.makeDir.all(directoryPath)
    val filePath = directoryPath / filename

    if (os.exists(filePath)) os.remove(filePath)
    FileUtils.writeToFileOverwrite(filePath, text.toCharArray.map(_.toByte))
  }

  /** Outputs the CSV file with the group address assignments for each communication object.
    *
    * @param filename the output filename. Defaults to "assignment.csv"
    */
  def outputGroupAddressesCsv(filename: String = "assignment.csv"): Unit = {
    def generateLine(assignment: Assignment) =
      List(
        "",
        "",
        s"${assignment.deviceAddress} - ${assignment.deviceName} - ${assignment.commObjectName}",
        assignment.groupAddress.toString,
        "",
        "",
        "",
        assignment.datatype.toString,
        "Auto"
      )

    val directoryPath = ASSIGNMENTS_DIRECTORY_PATH
    if (!os.exists(directoryPath)) os.makeDir.all(directoryPath)
    val filePath = directoryPath / filename

    val writer = CSVWriter.open(filePath.toIO)
    writer.writeRow(CSV_HEADER)
    writer.writeRow(CSV_MAIN_GROUP)
    writer.writeRow(CSV_MIDDLE_GROUP)
    writer.writeAll(assignments.map(generateLine))
    writer.close()
  }

  /** Outputs a .json file containing a mapping from group addresses to their physical ID
    * @param filename
    */
  def outputGroupAddressToPhysIdJson(filename: String = "gaToPhysId.json"): Unit = {
    val gaToPhysId: Map[String, Int] =
      assignment.physIdToGA.toList.map { case (i, address) =>
        (address.toString, i)
      }.toMap // We know this is valid because the GroupAddressAssignment class ensures that the map is reversible
    val pathToFile = ASSIGNMENTS_DIRECTORY_PATH / filename
    val toWrite = write(gaToPhysId)
    FileUtils.writeToFileOverwrite(pathToFile, toWrite.getBytes)
  }
}
