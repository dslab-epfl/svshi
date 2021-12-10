package ch.epfl.core.model.application

/**
 * Represents a library of applications for the platform, used during compilation
 * @param apps
 * @param path
 */
case class ApplicationLibrary(apps: List[Application], path: String)
