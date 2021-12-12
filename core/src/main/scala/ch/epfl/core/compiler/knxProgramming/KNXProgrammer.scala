package ch.epfl.core.compiler.knxProgramming

import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical.{GroupAddress, PhysicalDevice, PhysicalDeviceCommObject, PhysicalDeviceNode}
import ch.epfl.core.utils.Constants.ASSIGNMENTS_DIRECTORY_NAME

private case class Assignment(deviceAddress: String, nodeName: String, commObjectName: String, groupAddress: GroupAddress)

object Programmer {

  /** Outputs the programming file with the group address assignments for each communication object.
    *
    * @param assignment the group address assignment data structure
    * @param filename the output filename. Defaults to "assignment.txt"
    */
  def outputProgrammingFile(assignment: GroupAddressAssignment, filename: String = "assignment.txt"): Unit = {
    val idToGroupAddress = assignment.physIdToGA
    val assignments = assignment.physStruct.deviceInstances.flatMap { case PhysicalDevice(deviceName, (a1, a2, a3), nodes) =>
      val addressString = s"$a1.$a2.$a3"
      nodes.flatMap { case PhysicalDeviceNode(nodeName, comObjects) =>
        comObjects.filter(idToGroupAddress contains _.id).map { case PhysicalDeviceCommObject(objName, datatype, ioType, id) =>
          Assignment(addressString, nodeName, objName, idToGroupAddress(id))
        }
      }
    }
    val tree = assignments.groupBy(_.deviceAddress).map { case (deviceAddress, assignments) =>
      s"device address '$deviceAddress'\n" + assignments
        .groupBy(_.nodeName)
        .map { case (nodeName, assigns) =>
          s"  node '$nodeName'\n" + assigns.map(a => s"    comm object '${a.commObjectName}' => group address '${a.groupAddress}'").mkString("\n")
        }
        .mkString("\n")
    }
    val text = tree.mkString("\n")
    val directoryPath = os.pwd / os.up / ASSIGNMENTS_DIRECTORY_NAME
    if (!os.exists(directoryPath)) os.makeDir(directoryPath)
    val filePath = directoryPath / filename

    if (os.exists(filePath)) os.remove(filePath)
    os.write(filePath, text)
  }
}
