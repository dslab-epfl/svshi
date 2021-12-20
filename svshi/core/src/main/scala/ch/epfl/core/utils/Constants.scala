package ch.epfl.core.utils

import os.Path

object Constants {
  private val DEFAULT_SVSHI_HOME = s"${sys.env("HOME")}/smartinfra"
  var SVSHI_HOME: String = if (sys.env.contains("SVSHI_HOME")) sys.env("SVSHI_HOME") else DEFAULT_SVSHI_HOME

  def setSvshiHome(v: String): Unit = {
    SVSHI_HOME = v
  }
  val SVSHI_FOLDER = s"$SVSHI_HOME/svshi"
  val ASSIGNMENTS_DIRECTORY_NAME = s"$SVSHI_HOME/assignments"
  val PHYSICAL_STRUCTURE_JSON_FILE_NAME = "physical_structure.json"
  val APP_PROTO_BINDINGS_JSON_FILE_NAME = "apps_bindings.json"
  val APP_PYTHON_ADDR_BINDINGS_FILE_NAME = "addresses.json"
  val GROUP_ADDRESSES_LIST_FILE_NAME = "group_addresses.json"
  val APP_PROTO_STRUCT_FILE_NAME = "app_prototypical_structure.json"
  val GENERATED_FOLDER_PATH_STRING = s"$SVSHI_HOME/generated"
  val APP_LIBRARY_FOLDER_PATH_STRING = s"$SVSHI_FOLDER/app_library"
  val GENERATED_FOLDER_PATH: Path = os.Path(APP_LIBRARY_FOLDER_PATH_STRING)
  val APP_LIBRARY_FOLDER_PATH: Path = os.Path(APP_LIBRARY_FOLDER_PATH_STRING)
  val GENERATED_VERIFICATION_FILE_PATH: Path = os.Path(s"$SVSHI_FOLDER/verification/verification_file.py")
  val GENERATED_RUNTIME_FILE_PATH: Path = os.Path(s"$SVSHI_FOLDER/verification/runtime_file.py")
  val GENERATED_CONDITIONS_FILE_PATH: Path = os.Path(s"$SVSHI_FOLDER/verification/conditions.py")
  val VERIFICATION_PYTHON_MODULE = "verification.main"
  val APP_GENERATOR_PYTHON_MODULE = "generator.main"
  val RUNTIME_PYTHON_MODULE = "runtime.main"
  val RUNTIME_PYTHON_MODULE_PATH: Path = os.Path(s"$SVSHI_FOLDER/runtime")
  val CROSSHAIR_TIMEOUT_SECONDS = 600
}
