package ch.epfl.core.models.application

import ch.epfl.core.models.prototypical.AppPrototypicalStructure

case class Application(name: String, appFolderPath: String, appProtoStructure: AppPrototypicalStructure)
