package ch.epfl.core.compiler

import ch.epfl.core.models.ApplicationLibrary

object Compiler {
  def compile(library: ApplicationLibrary) : ApplicationLibrary = {
    //TODO
    // here we need to read assignment of physical communicationObjects to XKNX stuff
    // assign Group addresses to communicationObject
    // generate files for the python apps with the group addresses
    // generate files for the KNX programmming module
    library
  }

}
