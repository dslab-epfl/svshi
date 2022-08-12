package ch.epfl.web.service.main.utils

import os.Path

object Constants {
  lazy private val DEFAULT_SVSHI_WEBSERVICE_HOME =
    if (sys.env.contains("HOME")) s"${sys.env("HOME")}/svshi_webservice" else if (sys.env.contains("HOMEPATH")) s"${sys.env("HOMEPATH")}/svshi_webservice" else ""
  val SVSHI_WEBSERVICE_HOME_STR: String = if (sys.env.contains("SVSHI_WEBSERVICE_HOME")) sys.env("SVSHI_WEBSERVICE_HOME") else DEFAULT_SVSHI_WEBSERVICE_HOME
  val SVSHI_WEBSERVICE_HOME: os.Path = os.Path(SVSHI_WEBSERVICE_HOME_STR)
  val DEFAULT_VERSION = "0.1.0"
  val WEB_SERVICE_DEFAULT_PORT = 4545
  val WEB_SERVICE_DEFAULT_ADDRESS = "localhost"
  val SESSION_ID_COOKIE_NAME = "session_id"
  val SESSION_LIFESPAN_SECONDS: Int = 14 * 60 * 60 * 24 // 2 weeks
  val TEMP_FOLDER_PATH: Path = SVSHI_WEBSERVICE_HOME / "TEMP"
  val SESSIONS_FOLDER_PATH = SVSHI_WEBSERVICE_HOME / "sessions"
}
