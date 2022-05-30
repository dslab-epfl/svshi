package ch.epfl.core.model.application

import ch.epfl.core.model.prototypical.AppPrototypicalStructure

/** Represents an application written for the platform in Python, used during the compilation
  * @param name
  * @param appFolderPath
  * @param appProtoStructure
  */
case class Application(name: String, appFolderPath: os.Path, appProtoStructure: AppPrototypicalStructure, files: List[os.Path])
