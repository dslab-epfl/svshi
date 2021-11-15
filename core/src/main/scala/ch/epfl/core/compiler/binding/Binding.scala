package ch.epfl.core.compiler.binding

import ch.epfl.core.models.application.{Application, ApplicationLibrary}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, AppPrototypeBindings, BinarySensor, DeviceInstanceBinding, HumiditySensor, SupportedDevice, Switch, TemperatureSensor}

class Binding {
  def appLibraryBindingsFromLibrary(appLibrary: ApplicationLibrary): AppLibraryBindings = {
    def appToAppBinding(app: Application): AppPrototypeBindings = {
      AppPrototypeBindings(app.name, app.appProtoStructure.deviceInstances.map(inst => DeviceInstanceBinding(inst.name, SupportedDevice.getDeviceBinding(inst.deviceType))))
    }
    AppLibraryBindings(appLibrary.apps.map(appToAppBinding))
  }

}
