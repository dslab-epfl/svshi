package ch.epfl.web.service.main.utils

object Version {
  def getVersion(): String = {
    val version = getClass.getPackage.getImplementationVersion
    if (version != null) version else Constants.DEFAULT_VERSION
  }
}
