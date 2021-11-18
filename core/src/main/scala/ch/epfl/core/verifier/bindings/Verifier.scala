package ch.epfl.core.verifier.bindings

import ch.epfl.core.models.application.ApplicationLibrary
import ch.epfl.core.models.physical.{IOType, In, InOut, Out, PhysicalDevice, PhysicalStructure, Unknown}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, AppPrototypicalDeviceInstance, AppPrototypicalStructure, BinarySensorBinding, DeviceInstanceBinding, HumiditySensorBinding, SwitchBinding, TemperatureSensorBinding}
import ch.epfl.core.parsers.json.bindings.BindingsJsonParser
import ch.epfl.core.parsers.json.physical.PhysicalStructureJsonParser
import ch.epfl.core.parsers.json.prototype.{AppInputJsonParser, PrototypicalStructureJson}
import ch.epfl.core.utils.Constants
import ch.epfl.core.verifier.bindings.exceptions._

import java.nio.file.Path

object Verifier {

  def verify(appLibrary: ApplicationLibrary): ApplicationLibrary = {
    val physicalStructure = PhysicalStructureJsonParser.parse(Path.of(appLibrary.path).resolve(Constants.PHYSICAL_STRUCTURE_JSON_FILE_NAME).toString)
    val bindings = BindingsJsonParser.parse(Path.of(appLibrary.path).resolve(Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME).toString)
    val appPrototypicalStructures = appLibrary.apps.map(app => (app.name, AppInputJsonParser.parse(Path.of(app.appFolderPath).resolve(Constants.APP_PROTO_STRUCT_FILE_NAME).toString))).toMap
    verifyBindings(physicalStructure, bindings, appPrototypicalStructures)
    appLibrary
  }

  def verifyBindings(physicalStructure: PhysicalStructure, bindings: AppLibraryBindings, appPrototypicalStructures: Map[String, AppPrototypicalStructure]): Unit = {
    bindings.appBindings.foreach(binding => {
      val appName = binding.name
      val appProtoStructure = appPrototypicalStructures(appName)
      binding.bindings.foreach(deviceInstBinding => {
        val protoDevice = appProtoStructure.deviceInstances.find(d => d.name == deviceInstBinding.name).get
        deviceInstBinding.binding match {
          case BinarySensorBinding(typeString, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case SwitchBinding(typeString, writePhysDeviceId, readPhysDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, writePhysDeviceId)
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, readPhysDeviceId)
          }
          case TemperatureSensorBinding(typeString, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case HumiditySensorBinding(typeString, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
        }
      })
    })
  }

  private def checkPhysIdIOCompatibility(physicalStructure: PhysicalStructure, deviceInstBinding: DeviceInstanceBinding, protoDevice: AppPrototypicalDeviceInstance, physDeviceId: Int): Unit = {
    val typeIo = deviceInstBinding.binding.getIOTypes(physDeviceId)
    val physicalDeviceOpt: Option[PhysicalDevice] = getPhysicalDeviceByBoundId(physicalStructure, physDeviceId)
    if (physicalDeviceOpt.isEmpty) {
      throw new ErrorNotBoundToPhysicalDeviceException(s"The device name = ${deviceInstBinding.name} with physDeviceId = $physDeviceId is not bound to a physical device's communication object!")
    }
    val physicalDevice = physicalDeviceOpt.get
    val commObject = getCommObjectByBoundId(physDeviceId, physicalDevice).get
    // commObject IS DEFINED by construction
    checkCompatibilityIOTypes(typeIo, commObject.ioType, s"Proto device name = ${deviceInstBinding.name}, type = ${protoDevice.deviceType}; physical device address = ${physicalDevice.address}, commObject = ${commObject.name}, physicalId = ${commObject.id}")
  }

  private def checkCompatibilityIOTypes(ioType1: IOType, ioType2: IOType, msgDevicesDescription: String): Unit = {
    ioType1 match {
      case In => ioType2 match {
        case Out => throw new ErrorIOType(s"$msgDevicesDescription: type '$ioType1' is incompatible with type '$ioType2'!")
        case Unknown => throw new WarningIOType(s"$msgDevicesDescription: one ioType is Unknown, attention required!")
        case In =>
        case InOut =>
      }
      case Out =>  ioType2 match {
        case Unknown => throw new WarningIOType(s"$msgDevicesDescription: one ioType is Unknown, attention required!")
        case In => throw new ErrorIOType(s"$msgDevicesDescription: type '$ioType1' is incompatible with type '$ioType2'!")
        case Out =>
        case InOut =>
      }
      case InOut => ioType2 match {
        case Unknown => throw new WarningIOType(s"$msgDevicesDescription: one ioType is Unknown, attention required!")
        case In =>
        case Out =>
        case InOut =>
      }
      case Unknown => throw new WarningIOType(s"$msgDevicesDescription: one ioType is Unknown, attention required!")
    }
  }

  private def getCommObjectByBoundId(physDeviceId: Int, physicalDevice: PhysicalDevice) = {
    physicalDevice.nodes.flatMap(node => node.comObjects).find(cO => cO.id == physDeviceId)
  }

  private def getPhysicalDeviceByBoundId(physicalStructure: PhysicalStructure, physDeviceId: Int) = {
    val physicalDevice = physicalStructure.deviceInstances.find(d =>
      d.nodes.foldLeft(false)((acc, devNode) =>
        acc || devNode.comObjects.foldLeft(false)((acc, comObj) =>
          acc || comObj.id == physDeviceId)))
    physicalDevice
  }
}
