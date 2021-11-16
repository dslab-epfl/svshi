package ch.epfl.core.compiler.programming

import ch.epfl.core.models.physical.{PhysicalDevice, PhysicalDeviceNode, PhysicalDeviceCommObject, GroupAddress}
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.models.prototypical._

private case class Assignment(deviceAddress: String, nodeName: String, commObjectName: String, groupAddress: GroupAddress)

object Programmer {

  /** Outputs the programming file with the group address assignments for each communication object.
    *
    * @param assignment the group address assignment data structure
    * @param filename the output filename. Defaults to "assignment.txt"
    */
  def outputProgrammingFile(assignment: GroupAddressAssignment, filename: String = "assignment.txt") = {
    val idToGroupAddress = assignment.physIdToGA
    val assignments = assignment.physStruct.deviceInstances.flatMap { case PhysicalDevice(deviceName, address, nodes) =>
      val addressString = s"${address._1}.${address._2}.${address._3}"
      nodes.flatMap { case PhysicalDeviceNode(nodeName, comObjects) =>
        comObjects.map { case PhysicalDeviceCommObject(objName, datatype, ioType, id) =>
          Assignment(addressString, nodeName, objName, idToGroupAddress(id))
        }
      }
    }
    val tree = assignments.groupBy(_.deviceAddress).map { case (deviceAddress, assignments) =>
      s"device address $deviceAddress\n" + assignments
        .groupBy(_.nodeName)
        .map { case (nodeName, assigns) =>
          s"  node $nodeName\n" + assigns.map(a => s"    comm object ${a.commObjectName} => group address ${a.groupAddress}").mkString("\n")
        }
        .mkString("\n")
    }
    val text = tree.mkString("\n")
    val filePath = os.pwd / filename
    if (os.exists(filePath)) os.remove(filePath)
    os.write(filePath, text)
  }
}
