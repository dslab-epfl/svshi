package ch.epfl.core.compiler.bindings

import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.model.prototypical.{AppLibraryBindings, AppPrototypeBindings, BinarySensor, DeviceInstanceBinding, HumiditySensor, SupportedDevice, Switch, TemperatureSensor}
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.utils.Constants

import java.nio.file.Path

object Bindings {
  def appLibraryBindingsFromLibrary(newAppLibrary: ApplicationLibrary, existingAppLibrary: ApplicationLibrary, newAppPhysStruct: PhysicalStructure, existingPhysStruct: PhysicalStructure): AppLibraryBindings = {
    def appToAppBinding(app: Application): AppPrototypeBindings = {
      AppPrototypeBindings(app.name, app.appProtoStructure.deviceInstances.map(inst => DeviceInstanceBinding(inst.name, SupportedDevice.getDeviceBinding(inst.deviceType))))
    }
    val existingBindingsList = if(existingPhysStruct == newAppPhysStruct) {
      // We want to keep the old bindings so we append the new app's to the existing file
      val existingBindingsPath = Path.of(existingAppLibrary.path).resolve(Path.of(Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME))
      if(existingBindingsPath.toFile.exists()) {
        BindingsJsonParser.parse(existingBindingsPath.toString).appBindings
      }
      else {
        existingAppLibrary.apps.map(appToAppBinding)
      }
    } else {
      existingAppLibrary.apps.map(appToAppBinding)
    }

    AppLibraryBindings(existingBindingsList ++ newAppLibrary.apps.map(appToAppBinding))

  }

}
