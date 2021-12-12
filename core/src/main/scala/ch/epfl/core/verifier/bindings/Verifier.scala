package ch.epfl.core.verifier.bindings

import ch.epfl.core.model.application.ApplicationLibrary
import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.prototype.AppInputJsonParser
import ch.epfl.core.utils.Constants
import ch.epfl.core.verifier.VerifierTr
import ch.epfl.core.verifier.bindings.exceptions._

import java.nio.file.Path

/** Verifier that verifies properties about the bindings between prototypical devices and physical devices' communication objects
  */
object Verifier extends VerifierTr {

  /** Execute all verification on the libraries and output a list of messages
    * @param newAppLibrary
    * @param existingAppsLibrary
    * @param groupAddressAssignment
    * @return
    */
  def verify(newAppLibrary: ApplicationLibrary, existingAppsLibrary: ApplicationLibrary, groupAddressAssignment: GroupAddressAssignment): List[BindingsVerifierMessage] = {
    val combinedApps = newAppLibrary.apps ++ existingAppsLibrary.apps
    val physicalStructure = groupAddressAssignment.physStruct
    val bindings = groupAddressAssignment.appLibraryBindings
    val existingAppPrototypicalStructures =
      existingAppsLibrary.apps.map(app => (app.name, AppInputJsonParser.parse(Path.of(app.appFolderPath).resolve(Constants.APP_PROTO_STRUCT_FILE_NAME).toString))).toMap
    val newAppPrototypicalStructures =
      newAppLibrary.apps.map(app => (app.name, AppInputJsonParser.parse(Path.of(app.appFolderPath).resolve(Constants.APP_PROTO_STRUCT_FILE_NAME).toString))).toMap
    val appPrototypicalStructures = existingAppPrototypicalStructures ++ newAppPrototypicalStructures
    val returnValues = verifyBindingsIoTypes(physicalStructure, bindings, appPrototypicalStructures) ++
      verifyBindingsKNXDatatypes(physicalStructure, bindings, appPrototypicalStructures) ++
      verifyBindingsPythonType(groupAddressAssignment) ++
      verifyBindingsMutualDPT(bindings)
    returnValues
  }

  /** Verify that no group address is bound to channels with different datatypes in python
    * @param groupAddressAssignment
    * @return
    */
  def verifyBindingsPythonType(groupAddressAssignment: GroupAddressAssignment): List[BindingsVerifierMessage] = {
    groupAddressAssignment.getPythonTypesMap.toList.map { case (ga, pythonTypes) => (ga, pythonTypes.toSet) }.flatMap {
      case (_, pythonTypesSet) if pythonTypesSet.size == 1 => Nil
      case (ga, pythonTypesSet)                            => List(ErrorGroupAddressConflictingPythonTypes(s"groupAddress: $ga is bound to object channels with conflicting types: $pythonTypesSet"))
    }
  }

  /** Verify that the KNX datatypes of the communication objects corresponds to what is intended for each type of
    * prototypical device
    * @param physicalStructure
    * @param bindings
    * @param appPrototypicalStructures
    * @return
    */
  def verifyBindingsKNXDatatypes(
      physicalStructure: PhysicalStructure,
      bindings: AppLibraryBindings,
      appPrototypicalStructures: Map[String, AppPrototypicalStructure]
  ): List[BindingsVerifierMessage] = {
    bindings.appBindings.flatMap(binding => {
      val appName = binding.name
      val appProtoStructure = appPrototypicalStructures(appName)
      binding.bindings.flatMap(deviceInstBinding => {
        val protoDevice = appProtoStructure.deviceInstances.find(d => d.name == deviceInstBinding.name).get
        deviceInstBinding.binding match {
          case BinarySensorBinding(_, physDeviceId) => {
            checkPhysIdKNXDptCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case SwitchBinding(_, physDeviceId) => {
            checkPhysIdKNXDptCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case TemperatureSensorBinding(_, physDeviceId) => {
            checkPhysIdKNXDptCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case HumiditySensorBinding(_, physDeviceId) => {
            checkPhysIdKNXDptCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
        }
      })
    })
  }

  /** Verifies that IO types (i.e., in, out, in/out, unknown) of the physical communication objects are compatible
    * with intended types for each type of prototypical devices bound to them. Outputs WARNING for "unknown" types.
    *
    * @param physicalStructure
    * @param bindings
    * @param appPrototypicalStructures
    * @return
    */
  def verifyBindingsIoTypes(
      physicalStructure: PhysicalStructure,
      bindings: AppLibraryBindings,
      appPrototypicalStructures: Map[String, AppPrototypicalStructure]
  ): List[BindingsVerifierMessage] = {
    bindings.appBindings.flatMap(binding => {
      val appName = binding.name
      val appProtoStructure = appPrototypicalStructures(appName)
      binding.bindings.flatMap(deviceInstBinding => {
        val protoDevice = appProtoStructure.deviceInstances.find(d => d.name == deviceInstBinding.name).get
        deviceInstBinding.binding match {
          case BinarySensorBinding(_, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case SwitchBinding(_, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case TemperatureSensorBinding(_, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
          case HumiditySensorBinding(_, physDeviceId) => {
            checkPhysIdIOCompatibility(physicalStructure, deviceInstBinding, protoDevice, physDeviceId)
          }
        }
      })
    })
  }

  /** Verifies that all devices bound the same physicalID have the same KNX datatype
    * @param bindings
    * @return List of messagesg
    */
  def verifyBindingsMutualDPT(bindings: AppLibraryBindings): List[BindingsVerifierMessage] = {
    bindings.appBindings.flatMap(binding => {
      binding.bindings.flatMap(deviceInstBinding => {
        deviceInstBinding.binding match {
          case BinarySensorBinding(_, physDeviceId) => {
            checkMutualDPTCompatibility(deviceInstBinding, bindings, physDeviceId)
          }
          case SwitchBinding(_, physDeviceId) => {
            checkMutualDPTCompatibility(deviceInstBinding, bindings, physDeviceId)
          }
          case TemperatureSensorBinding(_, physDeviceId) => {
            checkMutualDPTCompatibility(deviceInstBinding, bindings, physDeviceId)
          }
          case HumiditySensorBinding(_, physDeviceId) => {
            checkMutualDPTCompatibility(deviceInstBinding, bindings, physDeviceId)
          }
        }
      })
    })
  }

  /** Verifies that no prototypical devices with different datatypes in python are bound to the same physical id
    * @param physicalStructure
    * @param deviceInstBinding
    * @param protoDevice
    * @param physDeviceId
    * @return
    */
  private def checkPhysIdKNXDptCompatibility(
      physicalStructure: PhysicalStructure,
      deviceInstBinding: DeviceInstanceBinding,
      protoDevice: AppPrototypicalDeviceInstance,
      physDeviceId: Int
  ): List[BindingsVerifierMessage] = {
    val knxDpt = deviceInstBinding.binding.getKNXDpt(physDeviceId)
    val physicalDeviceOpt: Option[PhysicalDevice] = getPhysicalDeviceByBoundId(physicalStructure, physDeviceId)
    if (physicalDeviceOpt.isEmpty) {
      return List(
        ErrorNotBoundToPhysicalDevice(s"The device name = ${deviceInstBinding.name} with physDeviceId = $physDeviceId is not bound to a physical device's communication object!")
      )
    }
    val physicalDevice = physicalDeviceOpt.get
    val commObject = getCommObjectByBoundId(physDeviceId, physicalDevice).get
    // commObject IS DEFINED by construction
    checkCompatibilityKNXTypes(
      knxDpt,
      commObject.datatype,
      s"Proto device name = ${deviceInstBinding.name}, type = ${protoDevice.deviceType}; physical device address = ${physicalDevice.address}, commObject = ${commObject.name}, physicalId = ${commObject.id}"
    )
  }

  private def checkMutualDPTCompatibility(deviceInstBinding: DeviceInstanceBinding, bindings: AppLibraryBindings, physDeviceId: Int): List[BindingsVerifierMessage] = {
    val others = boundProtoDevicesSamePhysId(bindings, physDeviceId)
    val msgBegin = s"Proto device name = ${deviceInstBinding.name}, physicalId = $physDeviceId; KNX DPT = ${deviceInstBinding.binding.getKNXDpt(physDeviceId)} conflicts with "
    others
      .filter(bind => bind.binding.getKNXDpt(physDeviceId) != deviceInstBinding.binding.getKNXDpt(physDeviceId))
      .map(b => ErrorProtoDevicesBoundSameIdDifferentDPT(s"$msgBegin Proto device name = ${b.name}, physicalId = $physDeviceId, KNX DPT = ${b.binding.getKNXDpt(physDeviceId)}"))
  }

  private def checkPhysIdIOCompatibility(
      physicalStructure: PhysicalStructure,
      deviceInstBinding: DeviceInstanceBinding,
      protoDevice: AppPrototypicalDeviceInstance,
      physDeviceId: Int
  ): List[BindingsVerifierMessage] = {
    val protoTypeIo = deviceInstBinding.binding.getIOTypes(physDeviceId)
    val physicalDeviceOpt: Option[PhysicalDevice] = getPhysicalDeviceByBoundId(physicalStructure, physDeviceId)
    if (physicalDeviceOpt.isEmpty) {
      return List(
        ErrorNotBoundToPhysicalDevice(s"The device name = ${deviceInstBinding.name} with physDeviceId = $physDeviceId is not bound to a physical device's communication object!")
      )
    }
    val physicalDevice = physicalDeviceOpt.get
    val physCommObject = getCommObjectByBoundId(physDeviceId, physicalDevice).get
    // physCommObject IS DEFINED by construction
    checkCompatibilityIOTypes(
      protoTypeIo,
      physCommObject.ioType,
      s"Proto device name = ${deviceInstBinding.name}, type = ${protoDevice.deviceType}; physical device address = ${physicalDevice.address}, commObject = ${physCommObject.name}, physicalId = ${physCommObject.id}"
    )
  }

  private def checkCompatibilityKNXTypes(dpt1: KNXDatatype, dpt2: KNXDatatype, msgDevicesDescription: String): List[BindingsVerifierMessage] = {
    dpt1 match {
      case DPTUnknown => List(WarningKNXDatatype(s"$msgDevicesDescription: one KNXDatatype is UnknownDPT, attention required!"))
      case _ =>
        dpt2 match {
          case DPTUnknown        => List(WarningKNXDatatype(s"$msgDevicesDescription: one KNXDatatype is UnknownDPT, attention required!"))
          case _ if dpt1 == dpt2 => Nil
          case _                 => List(ErrorKNXDatatype(s"$msgDevicesDescription: KNXDatatype '$dpt1' is incompatible with KNXDatatype '$dpt2'!"))
        }
    }
  }

  private def checkCompatibilityIOTypes(protoIOType: IOType, physIOType: IOType, msgDevicesDescription: String): List[BindingsVerifierMessage] = {
    protoIOType match {
      case In =>
        physIOType match {
          case Unknown => List(WarningIOType(s"$msgDevicesDescription: physicalIOType is Unknown, attention required!"))
          case _       => Nil
        }
      case Out =>
        physIOType match {
          case Unknown => List(WarningIOType(s"$msgDevicesDescription: physicalIOType is Unknown, attention required!"))
          case In      => List(ErrorIOType(s"$msgDevicesDescription: protoIOType '$protoIOType' is incompatible with physicalIOType '$physIOType'!"))
          case _       => Nil
        }
      case InOut =>
        physIOType match {
          case InOut   => Nil
          case Out     => Nil
          case Unknown => List(WarningIOType(s"$msgDevicesDescription: physicalIOType is Unknown, attention required!"))
          case _       => List(ErrorIOType(s"$msgDevicesDescription: protoIOType '$protoIOType' is incompatible with physicalIOType '$physIOType'!"))
        }
      case Unknown => List(WarningIOType(s"$msgDevicesDescription: protoIOType is Unknown, attention required!"))
    }
  }

  private def boundProtoDevicesSamePhysId(bindings: AppLibraryBindings, physDeviceId: Int): List[DeviceInstanceBinding] = {
    bindings.appBindings.flatMap(appProtoBind => appProtoBind.bindings.flatMap(bind => List(bind).filter(b => b.binding.getBoundIds.contains(physDeviceId))))
  }

  private def getCommObjectByBoundId(physDeviceId: Int, physicalDevice: PhysicalDevice): Option[PhysicalDeviceCommObject] = {
    physicalDevice.nodes.flatMap(node => node.comObjects).find(cO => cO.id == physDeviceId)
  }

  private def getPhysicalDeviceByBoundId(physicalStructure: PhysicalStructure, physDeviceId: Int): Option[PhysicalDevice] = {
    val physicalDevice = physicalStructure.deviceInstances.find(d =>
      d.nodes.foldLeft(false)((acc, devNode) => acc || devNode.comObjects.foldLeft(false)((acc, comObj) => acc || comObj.id == physDeviceId))
    )
    physicalDevice
  }
}
