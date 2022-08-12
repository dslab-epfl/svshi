package ch.epfl.web.service.main.svshi
import ch.epfl.web.service.main.utils.FileUtils.zipInMem
import ch.epfl.web.service.main.utils.Utils.JsonParsingException
import os.Path
import upickle.default._

case class SvshiHttp(svshiAddress: String, svshiPort: Int) extends SvshiInterface {
  val READ_SVSHI_REQUEST_TIMEOUT = 600_000 // Milliseconds
  override def getDeviceMappings(etsProjFile: Path): String = {
    // Send the request to SVSHI
    val data = zipInMem(List(etsProjFile)) match {
      case Some(value) => value
      case None        => throw new RuntimeException("Cannot create an in-memory zip for the ETS File!")
    }
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/deviceMappings",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT,
      data = zipInMem(List(etsProjFile)).get
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot get the mappings from SVSHI! Here is the error:\n${r.text()}")
    }
    r.text()
  }

  override def generateApp(appName: String, jsonProto: String): Array[Byte] = {
    // Send the request to SVSHI
    val rGen = requests.post(
      f"http://$svshiAddress:$svshiPort/generateApp/$appName",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT,
      data = jsonProto
    )
    if (rGen.statusCode != 200) {
      throw new RuntimeException(s"Cannot generate an app on SVSHI! Here is the error:\n${rGen.text()}")
    }
    val responseBodyGen = ResponseBody.from(rGen.text())
    if (!responseBodyGen.status) {
      throw new RuntimeException(s"Cannot generate an app on SVSHI! Here is the error:\n${responseBodyGen.output.mkString("\n")}")
    }
    // The app is in the "generated" folder of SVSHI.
    // We need to download it and delete it.

    val rDownload = requests.get(
      f"http://$svshiAddress:$svshiPort/generated/$appName",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (rDownload.statusCode != 200) {
      throw new RuntimeException(s"Cannot download the new app from SVSHI! Here is the error:\n${rDownload.text()}")
    }
    val appZipData = rDownload.data.array
    val rDelete = requests.post(
      f"http://$svshiAddress:$svshiPort/deleteGenerated/$appName",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (rDelete.statusCode != 200) {
      throw new RuntimeException(s"Cannot delete the generated app on SVSHI! Here is the error:\n${rDelete.text()}")
    }
    val responseBodyDelete = ResponseBody.from(rGen.text())
    if (!responseBodyDelete.status) {
      throw new RuntimeException(s"Cannot delete the app on SVSHI! Here is the error:\n${responseBodyDelete.output.mkString("\n")}")
    }

    appZipData

  }

  /** Get the list of names of installedApps on SVSHI
    *
    * @return a List containing the names of the apps
    */
  override def getInstalledAppNames(): List[String] = {
    val r = requests.get(
      f"http://$svshiAddress:$svshiPort/listApps",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot download the new app from SVSHI! Here is the error:\n${r.text()}")
    }
    ResponseBody.from(r.text()).output
  }

  override def uninstallAllApps(): (Boolean, List[String]) = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/removeAllApps",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot uninstall apps from SVSHI! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    (responseBody.status, responseBody.output)
  }

  override def addToGenerated(zipArchive: Array[Byte]): Unit = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/generated",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT,
      data = zipArchive
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot put content in generated folder on SVSHI! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    if (!responseBody.status) {
      throw new RuntimeException(s"Cannot put content in generated folder on SVSHI! Here is the error:\n${responseBody.output.mkString("\n")}")
    }
  }

  override def removeAllFromGenerated(): Unit = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/deleteAllGenerated",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot delete everything from generated folder on SVSHI! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    if (!responseBody.status) {
      throw new RuntimeException(s"Cannot delete everything from generated folder on SVSHI! Here is the error:\n${responseBody.output.mkString("\n")}")
    }
  }

  override def generateBindings(etsFileAsZip: Array[Byte]): (Boolean, List[String]) = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/generateBindings",
      check = false,
      data = etsFileAsZip,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot generateBindings on SVSHI! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    (responseBody.status, responseBody.output)
  }

  override def compile(etsFileAsZip: Array[Byte]): (Boolean, List[String]) = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/compile",
      check = false,
      data = etsFileAsZip,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot compile on SVSHI! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    (responseBody.status, responseBody.output)
  }

  override def run(ip: String, port: Int)(debug: String => Unit = s => ()): Unit = {
    val ipPort = f"$ip:$port"
    debug(s"Send request to svshi $svshiAddress:$svshiPort/run/$ipPort")
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/run/$ipPort",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot run SVSHI! Here is the error:\n${r.text()}")
    }
  }

  override def stop(): Unit = {
    val r = requests.post(
      f"http://$svshiAddress:$svshiPort/stopRun",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot stop SVSHI run! Here is the error:\n${r.text()}")
    }
  }

  override def getRunStatus(): Boolean = {
    val r = requests.get(
      f"http://$svshiAddress:$svshiPort/runStatus",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot get the SVSHI runStatus! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    responseBody.status
  }

  override def getRunLogs(): List[String] = {
    val r = requests.get(
      f"http://$svshiAddress:$svshiPort/logs/run",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot get the SVSHI run logs! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    if (!responseBody.status) Nil else responseBody.output
  }

  override def getReceivedTelegramsLogs(): List[String] = {
    val r = requests.get(
      f"http://$svshiAddress:$svshiPort/logs/receivedTelegrams",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot get the SVSHI receivedTelegrams logs! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    if (!responseBody.status) Nil else responseBody.output
  }

  override def getExecutionLogs(): List[String] = {
    val r = requests.get(
      f"http://$svshiAddress:$svshiPort/logs/execution",
      check = false,
      readTimeout = READ_SVSHI_REQUEST_TIMEOUT
    )
    if (r.statusCode != 200) {
      throw new RuntimeException(s"Cannot get the SVSHI execution logs! Here is the error:\n${r.text()}")
    }
    val responseBody = ResponseBody.from(r.text())
    if (!responseBody.status) Nil else responseBody.output
  }

  override def getParserPhysicalStructureJson(etsFileZip: Array[Byte]): String = {
    val r = requests.post(f"http://$svshiAddress:$svshiPort/physicalStructure/parsed", check = false, data = etsFileZip, readTimeout = READ_SVSHI_REQUEST_TIMEOUT)
    if (r.statusCode != 200) throw new RuntimeException(s"Cannot get the parsed physicalStructure! See error:\n${r.text()}")
    r.text()
  }

  override def getAppLibraryBindings(): String = {
    val r = requests.get(f"http://$svshiAddress:$svshiPort/appLibrary/bindings", check = false, readTimeout = READ_SVSHI_REQUEST_TIMEOUT)
    if (r.statusCode != 200) "" else r.text()
  }

  override def getAssignmentGaToPhysId(): Map[String, Int] = {
    val r = requests.get(f"http://$svshiAddress:$svshiPort/assignments/gaToPhysId", check = false, readTimeout = READ_SVSHI_REQUEST_TIMEOUT)
    if (r.statusCode != 200) Map() else read[Map[String, Int]](r.text())

  }
}

/** Classes used by upickle in the CoreApiServer
  */

case class ResponseBody(status: Boolean, output: List[String]) {
  override def toString: String = write(this, indent = 2)
}
object ResponseBody {
  implicit val responseBody: ReadWriter[ResponseBody] = macroRW[ResponseBody]
  def from(s: String): ResponseBody = try {
    upickle.default.read[ResponseBody](s)
  } catch {
    case e: Exception =>
      throw new JsonParsingException(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\nThe following exception was thrown $e")
  }
}
