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
            checkOnePhysIdDevice(physicalStructure, deviceInstBinding, protoDevice, typeString, physDeviceId)
          }
          case SwitchBinding(typeString, writePhysDeviceId, readPhysDeviceId) => {
            val writeTypeIo = deviceInstBinding.binding.getIOTypes(writePhysDeviceId)
            val writePhysicalDeviceOpt: Option[PhysicalDevice] = getPhysicalDeviceByBoundId(physicalStructure, writePhysDeviceId)
            if (writePhysicalDeviceOpt.isEmpty) {
              throw new ErrorNotBoundToPhysicalDeviceException(s"The device name = ${deviceInstBinding.name} with writePhysDeviceId = $writePhysDeviceId is not bound to a physical device's communication object!")
            }
            val writePhysicalDevice = writePhysicalDeviceOpt.get
            val writeCommObject = getCommObjectByBoundId(writePhysDeviceId, writePhysicalDevice).get
            // commObject IS DEFINED by construction
            checkCompatibilityIOTypes(writeTypeIo, writeCommObject.ioType, s"Proto device name = ${deviceInstBinding.name}, type = ${protoDevice.deviceType}, writePhysDeviceId; physical device address = ${writePhysicalDevice.address}, commObject = ${writeCommObject.name}, physicalId = ${writeCommObject.id}")

            val readTypeIo = deviceInstBinding.binding.getIOTypes(readPhysDeviceId)
            val readPhysicalDeviceOpt: Option[PhysicalDevice] = getPhysicalDeviceByBoundId(physicalStructure, readPhysDeviceId)
            if (readPhysicalDeviceOpt.isEmpty) {
              throw new ErrorNotBoundToPhysicalDeviceException(s"The device name = ${deviceInstBinding.name} with readPhysDeviceId = $readPhysDeviceId is not bound to a physical device's communication object!")
            }
            val readPhysicalDevice = readPhysicalDeviceOpt.get
            val readCommObject = getCommObjectByBoundId(readPhysDeviceId, readPhysicalDevice).get
            // commObject IS DEFINED by construction
            checkCompatibilityIOTypes(readTypeIo, readCommObject.ioType, s"Proto device name = ${deviceInstBinding.name}, type = ${protoDevice.deviceType}, readPhysDeviceId; physical device address = ${readPhysicalDevice.address}, commObject = ${readCommObject.name}, physicalId = ${readCommObject.id}")

          }
          case TemperatureSensorBinding(typeString, physDeviceId) => {
            checkOnePhysIdDevice(physicalStructure, deviceInstBinding, protoDevice, typeString, physDeviceId)
          }
          case HumiditySensorBinding(typeString, physDeviceId) => {
            checkOnePhysIdDevice(physicalStructure, deviceInstBinding, protoDevice, typeString, physDeviceId)
          }
        }
      })
    })
  }

  private def checkOnePhysIdDevice(physicalStructure: PhysicalStructure, deviceInstBinding: DeviceInstanceBinding, protoDevice: AppPrototypicalDeviceInstance, typeString: String, physDeviceId: Int): Unit = {
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
