package ch.epfl.core.parser.json.bindings

import ch.epfl.core.model.application.Application
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical.GroupAddress
import ch.epfl.core.model.prototypical._
import upickle.default.write

import java.nio.charset.StandardCharsets

/** Parser used to produces a JSON file containing group addresses bindings for the runtime module
  */
object PythonAddressJsonParser {

  /** Write to a JSON file the bindings in a form used by the runtime python module
    * @param filePath
    * @param pythonAddress
    */
  def writeToFile(filePath: os.Path, pythonAddress: AppPythonAddressesJson): Unit = {
    if(os.exists(filePath)) os.remove.all(filePath) // So that we get a fresh copy
    os.write(filePath, write(pythonAddress, indent = 2) getBytes StandardCharsets.UTF_8)
  }

  /** Produce the group addresses for the python runtime
    * @param app
    * @param assignment
    * @return
    */
  def assignmentToPythonAddressJson(app: Application, assignment: GroupAddressAssignment): AppPythonAddressesJson = {
    def createAddressJsonFromBinding(instBinding: DeviceInstanceBinding, physIdToGA: Map[Int, GroupAddress]): DeviceAddressJson = {
      instBinding.binding match {
        case BinarySensorBinding(_, physDeviceId)      => BinarySensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
        case SwitchBinding(_, physDeviceId)            => SwitchAddressJson(instBinding.name, physIdToGA(physDeviceId).toString, physIdToGA(physDeviceId).toString)
        case TemperatureSensorBinding(_, physDeviceId) => TemperatureSensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
        case HumiditySensorBinding(_, physDeviceId)    => HumiditySensorAddressJson(instBinding.name, physIdToGA(physDeviceId).toString)
      }
    }
    val applicationBindingsOpt = assignment.appLibraryBindings.appBindings.find(b => b.name == app.name)
    if (applicationBindingsOpt.isEmpty) throw new IllegalArgumentException("The given application is not present in the assignment.")
    val applicationBindings = applicationBindingsOpt.get
    AppPythonAddressesJson(
      permissionLevel = app.appProtoStructure.permissionLevel.toString,
      addresses = applicationBindings.bindings.map(createAddressJsonFromBinding(_, assignment.physIdToGA))
    )
  }

}
