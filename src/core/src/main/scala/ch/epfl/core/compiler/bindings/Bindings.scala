package ch.epfl.core.compiler.bindings

import ch.epfl.core.model.application.{Application, ApplicationLibrary}
import ch.epfl.core.model.physical.PhysicalStructure
import ch.epfl.core.model.prototypical.{AppLibraryBindings, AppPrototypeBindings, DeviceInstanceBinding, SupportedDevice}
import ch.epfl.core.parser.json.bindings.BindingsJsonParser
import ch.epfl.core.utils.Constants

object Bindings {

  /** Generate bindings files for a library of existing applications and a folder of new applications to be added.
    * If a bindings file exist in the existing applications library and the physical structure used for this compilation
    * is the same as the one stored in this library, the bindings are read and put back in the new bindings file, adding
    * new empty ones for the new applications.
    * If that's not the case, new bindings are generated for all applications in both folders.
    * @param newAppLibrary
    * @param existingAppLibrary
    * @param newAppPhysStruct
    * @param existingPhysStruct
    * @return The AppLibraryBindings instance produced
    */
  def appLibraryBindingsFromLibrary(
      newAppLibrary: ApplicationLibrary,
      existingAppLibrary: ApplicationLibrary,
      newAppPhysStruct: PhysicalStructure,
      existingPhysStruct: PhysicalStructure
  ): AppLibraryBindings = {
    def appToAppBinding(app: Application): AppPrototypeBindings = {
      AppPrototypeBindings(app.name, app.appProtoStructure.deviceInstances.map(inst => DeviceInstanceBinding(inst.name, SupportedDevice.getDeviceBinding(inst))))
    }
    val existingBindingsList = if (existingPhysStruct == newAppPhysStruct) {
      // We want to keep the old bindings so we append the new app's to the existing file
      val existingBindingsPath = existingAppLibrary.path / Constants.APP_PROTO_BINDINGS_JSON_FILE_NAME
      if (os.exists(existingBindingsPath)) {
        BindingsJsonParser.parse(existingBindingsPath).appBindings
      } else {
        existingAppLibrary.apps.map(appToAppBinding)
      }
    } else {
      existingAppLibrary.apps.map(appToAppBinding)
    }

    // We remove the bindings if the application is not in the library anymore
    AppLibraryBindings(existingBindingsList.filter(b => existingAppLibrary.apps.exists(a => a.name == b.name)) ++ newAppLibrary.apps.map(appToAppBinding))

  }

}
