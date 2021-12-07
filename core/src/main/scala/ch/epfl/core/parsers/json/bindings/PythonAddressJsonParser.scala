package ch.epfl.core.parsers.json.bindings

import ch.epfl.core.models.application.Application
import ch.epfl.core.models.bindings.GroupAddressAssignment
import ch.epfl.core.models.physical.{GroupAddress, PhysicalStructure}
import ch.epfl.core.models.prototypical.{BinarySensorBinding, DeviceInstanceBinding, HumiditySensorBinding, SwitchBinding, TemperatureSensorBinding}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import upickle.default.write

object PythonAddressJsonParser {
  def writeToFile(filePath: String, pythonAddress: AppPythonAddressesJson): Unit = {
  val f = Paths.get(filePath).toFile
  if (f.exists()) f.delete() // So that we get a fresh copy
  Files.write(Paths.get(filePath), write(pythonAddress, indent = 2) getBytes StandardCharsets.UTF_8)
}

  def assignmentToPythonAddressJson(app: Application, assignment: GroupAddressAssignment): AppPythonAddressesJson = {
    def createAddressJsonFromBinding(instBinding: DeviceInstanceBinding, physIdToGA: Map[Int, GroupAddress]): DeviceAddressJson = {
      instBinding.binding match {
        case BinarySensorBinding(_, physDeviceId) => BinarySensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
        case SwitchBinding(_, physDeviceId) => SwitchAddressJson(instBinding.name, physIdToGA(physDeviceId).toString, physIdToGA(physDeviceId).toString)
        case TemperatureSensorBinding(_, physDeviceId) => TemperatureSensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
        case HumiditySensorBinding(_, physDeviceId) => HumiditySensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
      }
    }
    val applicationBindingsOpt = assignment.appLibraryBindings.appBindings.find(b => b.name == app.name)
    if(applicationBindingsOpt.isEmpty) throw new IllegalArgumentException("The given application is not present in the assignment.")
    val applicationBindings = applicationBindingsOpt.get
    AppPythonAddressesJson(permissionLevel = app.appProtoStructure.permissionLevel.toString, addresses = applicationBindings.bindings.map(createAddressJsonFromBinding(_,assignment.physIdToGA)))
  }


}
