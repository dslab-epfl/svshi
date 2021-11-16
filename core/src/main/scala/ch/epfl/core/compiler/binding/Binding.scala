package ch.epfl.core.compiler.binding

import ch.epfl.core.models.application.{Application, ApplicationLibrary}
import ch.epfl.core.models.prototypical.{AppLibraryBindings, AppPrototypeBindings, BinarySensor, DeviceInstanceBinding, HumiditySensor, SupportedDevice, Switch, TemperatureSensor}

object Binding {
  def appLibraryBindingsFromLibrary(newApps: ApplicationLibrary, appLibrary: ApplicationLibrary): AppLibraryBindings = {
    def appToAppBinding(app: Application): AppPrototypeBindings = {
      AppPrototypeBindings(app.name, app.appProtoStructure.deviceInstances.map(inst => DeviceInstanceBinding(inst.name, SupportedDevice.getDeviceBinding(inst.deviceType))))
    }
    AppLibraryBindings(appLibrary.apps.appendedAll(newApps.apps).map(appToAppBinding))
  }

}
