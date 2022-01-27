package ch.epfl.core.parser.ets

import ch.epfl.core.model.physical
import ch.epfl.core.model.physical._
import ch.epfl.core.utils.FileUtils._

import java.io.{File, FileNotFoundException}
import scala.collection.mutable
import scala.collection.parallel.CollectionConverters._
import scala.util.matching.Regex
import scala.xml.{Elem, Node, XML}

object EtsParser {
  val FILE_0_XML_NAME = "0.xml"
  val HARDWARE_XML_NAME = "Hardware.xml"
  val ADDRESS_PARAM = "Address"
  val AREA_TAG = "Area"
  val LINE_TAG = "Line"
  val DEVICE_TAG = "DeviceInstance"
  val PRODUCT_TAG = "Product"
  val ID_PARAM = "Id"
  val TEXT_PARAM = "Text"
  val PRODUCTREFID_PARAM = "ProductRefId"
  val NODE_TAG = "Node"
  val NODES_TAG = "Nodes"
  val GROUPOBJECTINSTANCES_PARAM = "GroupObjectInstances"
  val GROUPOBJECTTREE_TAG = "GroupObjectTree"
  val TYPE_PARAM = "Type"
  val REFID_PARAM = "RefId"
  val HARDWARE2PROGRAMREFID_PARAM = "Hardware2ProgramRefId"
  val COMOBJECTREF_TAG = "ComObjectRef"
  val COMOBJECT_TAG = "ComObject"
  val DATAPOINTTYPE_PARAM = "DatapointType"
  val OBJECTSIZE_PARAM = "ObjectSize"
  val NAME_PARAM = "Name"
  val READFLAG_PARAM = "ReadFlag"
  val WRITEFLAG_PARAM = "WriteFlag"
  val COMMUNICATIONFLAG_PARAM = "CommunicationFlag"
  val TRANSMITFLAG_PARAM = "TransmitFlag"
  val UPDATEFLAG_PARAM = "UpdateFlag"
  val READONINITFLAG_PARAM = "ReadOnInitFlag"
  val FUNCTIONTEXT_PARAM = "FunctionText"
  val LANGUAGE_TAG = "Language"
  val IDENTIFIER_PARAM = "Identifier"
  val TRANSLATIONELEMENT_TAG = "TranslationElement"
  val ATTRIBUTENAME_PARAM = "AttributeName"
  val TRANSLATION_TAG = "Translation"
  val APPLICATIONPROGRAM_TAG = "ApplicationProgram"
  val enUsLanguageCode = "en-US"

  val enabledText = "Enabled"
  val disabledText = "Disabled"

  val etsDpstRegex: Regex = "DPST-[0-9]+".r
  val etsDptRegex: Regex = "DPT-[0-9]+".r

  private val tempFolderPath = getPathFromSvshiHome("temp")
  private val defaultNodeName = "Default"

  private val xmlFilesCache = mutable.Map[File, scala.xml.Elem]()

  /** Parse an ETS project and produce a PhysicalStructure instance
    * @param etsProjectPath
    * @return
    */
  def parseEtsProjectFile(etsProjectPath: os.Path): PhysicalStructure = extractIfNotExist(
    etsProjectPath,
    _ => {
      val deviceAddresses = explore0xmlFindListAddresses(etsProjectPath)
      val parsedDevices = deviceAddresses.par.map(readDeviceFromEtsFile(etsProjectPath, _))
      val res = physical.PhysicalStructure(parsedDevices.map(parsedDeviceToPhysicalDevice).toList)
      deleteUnzippedFiles(etsProjectPath)
      res
    }
  )

  private def parsedDeviceToPhysicalDevice(parsedDevice: ParsedDevice): PhysicalDevice = {

    /** DPT types can be either format DPST-X-Y or DPT-X
      * The id is the hash using MurmurHash3 on List(name, datatype, ioType)
      * @param parsedioPort
      * @return
      */
    def ioPortToPhysicalChannel(parsedioPort: CommObject, physicalAddress: (String, String, String), deviceNodeName: String): PhysicalDeviceCommObject = {
      val dpstOpt = etsDpstRegex.findFirstIn(parsedioPort.dpt)
      val dptOpt = etsDptRegex.findFirstIn(parsedioPort.dpt)
      val datatype =
        if (dpstOpt.isDefined)
          KNXDatatype.fromString(dpstOpt.get.replace("S", ""))
        else if (dptOpt.isDefined) KNXDatatype.fromString(dptOpt.get)
        else if (KNXDatatype.fromDPTSize(parsedioPort.objectSizeString).isDefined) KNXDatatype.fromDPTSize(parsedioPort.objectSizeString)
        else if (parsedioPort.dpt == "") Some(DPTUnknown)
        else
          throw new MalformedXMLException(
            s"The DPT is not formatted as $etsDptRegex or $etsDpstRegex (or empty String) and the ObjectSize is not convertible to DPT (objectSize = ${parsedioPort.objectSizeString}) for the IOPort $parsedioPort for the device with address ${parsedDevice.address}"
          )
      val ioType = IOType.fromString(parsedioPort.inOutType).get
      PhysicalDeviceCommObject.from(parsedioPort.name, datatype.getOrElse(DPTUnknown), ioType, physicalAddress, deviceNodeName)
    }
    physical.PhysicalDevice(
      parsedDevice.name,
      parsedDevice.address,
      parsedDevice.io.map(parsedNode => PhysicalDeviceNode(parsedNode.name, parsedNode.ioPorts.map(ioPortToPhysicalChannel(_, parsedDevice.address, parsedNode.name))))
    )
  }

  /** Read one device from the ETS xml project
    *
    * It assumes that the project contains only one installation
    *
    * @param etsProjectPath the path to the etsProject file as os.Path
    * @param deviceAddress  the address of the device in topology of the project e.g., 1.1.1
    */
  private def readDeviceFromEtsFile(etsProjectPath: os.Path, deviceAddress: (String, String, String)): ParsedDevice = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      val deviceInstanceXMLOpt: Option[Node] = getDeviceInstanceIn0Xml(deviceAddress, projectRootPath)
      deviceInstanceXMLOpt match {
        case Some(deviceInstanceXML) => {
          val productRefId = deviceInstanceXML \@ PRODUCTREFID_PARAM
          val hardware2ProgramRefId = deviceInstanceXML \@ HARDWARE2PROGRAMREFID_PARAM
          val deviceName = getDeviceNameInCatalog(etsProjectPath, productRefId, hardware2ProgramRefId)
          val inOut = getDeviceCommObjectsInCatalog(etsProjectPath, deviceAddress)
          ParsedDevice(deviceAddress, deviceName, inOut)
        }
        case None => throw new MalformedXMLException(s"Cannot find the XML specific to $deviceAddress in $etsProjectPath")
      }
    }
  )

  private def takeFromCacheOrComputeAndUpdate(file: File): Elem = {
    if (xmlFilesCache.contains(file)) xmlFilesCache(file)
    else {
      val xml = XML.loadFile(file)
      xmlFilesCache.put(file, xml)
      xml
    }
  }

  private def getDeviceInstanceIn0Xml(deviceAddress: (String, String, String), projectRootPath: os.Path): Option[Node] = {
    val file0XmlPath = recursiveListFiles(projectRootPath).find(file => file.toIO.getName == FILE_0_XML_NAME)
    if (file0XmlPath.isEmpty) throw new MalformedXMLException("Missing 0.xml")
    val (areaN, lineN, deviceN) = deviceAddress
    val doc0xml = takeFromCacheOrComputeAndUpdate(file0XmlPath.get.toIO)
    val deviceInstanceXMLOpt =
      (((doc0xml \\ AREA_TAG).par.find(a => a \@ ADDRESS_PARAM == areaN).get \\ LINE_TAG).par.find(l => l \@ ADDRESS_PARAM == lineN).get \\ DEVICE_TAG).par.find(d =>
        d \@ ADDRESS_PARAM == deviceN
      )
    deviceInstanceXMLOpt
  }

  /** Get the name of the device in the catalog entry xml file (ApplicationProgram object)
    *
    * @param etsProjectPath the path to the etsProject file as os.Path
    * @param productRefId   the productRefId of the device
    * @return
    */
  private def getDeviceNameInCatalog(etsProjectPath: os.Path, productRefId: String, hardware2ProgramRefId: String): String = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      val hardwareXmlPath = hardwareXmlFile(etsProjectPath, productRefId, hardware2ProgramRefId)
      val hardwareEntry = takeFromCacheOrComputeAndUpdate(hardwareXmlPath.toIO)
      val productEntryOpt = (hardwareEntry \\ PRODUCT_TAG).find(n => (n \@ ID_PARAM) == productRefId)
      val name = productEntryOpt match {
        case Some(value) => getTranslation(hardwareEntry, enUsLanguageCode, productRefId, TEXT_PARAM).getOrElse(value \@ TEXT_PARAM)
        case None        => ""
      }
      if (name.isBlank) throw new MalformedXMLException(s"Cannot find the product name in Hardware.xml for the productRefId = $productRefId")
      name

    }
  )

  private def getApplicationProgramNode(productRefId: String, xmlPath: os.Path, catalogEntry: Elem) = {
    val applicationProgramList = catalogEntry \\ APPLICATIONPROGRAM_TAG
    if (applicationProgramList.length > 1) {
      throw new MalformedXMLException(
        s"The catalog applicationProgram for the device with productRefId=$productRefId has more than one ApplicationProgram object in the xml (file=$xmlPath)!"
      )
    }
    if (applicationProgramList.length < 1) {
      throw new MalformedXMLException(s"The catalog applicationProgram for the device with productRefId=$productRefId has no ApplicationProgram object in the xml (file=$xmlPath)!")
    }
    val applicationProgram = applicationProgramList.head
    applicationProgram
  }

  private def getDeviceCommObjectsInCatalog(etsProjectPath: os.Path, deviceAddress: (String, String, String)): List[ChannelNode] = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      def constructChannelNodeName(n: Node, productRefId: String, hardware2ProgramRefId: String) = {
        val xmlPath = productCatalogXMLFile(etsProjectPath, productRefId, hardware2ProgramRefId)
        val catalogEntry = takeFromCacheOrComputeAndUpdate(xmlPath.toIO)
        val applicationProgram: Node = getApplicationProgramNode(productRefId, xmlPath, catalogEntry)
        val appProgramId = applicationProgram \@ ID_PARAM
        val refId = n \@ REFID_PARAM
        val channelId = appProgramId + "_" + refId
        val typeText = getTranslation(catalogEntry, enUsLanguageCode, channelId, TYPE_PARAM).getOrElse(n \@ TYPE_PARAM)
        val textText = getTranslation(catalogEntry, enUsLanguageCode, channelId, TEXT_PARAM).getOrElse(n \@ TEXT_PARAM)
        List(typeText, refId, textText).filterNot(_ == "").mkString(" - ")
      }

      val deviceInstanceXMLOpt: Option[Node] = getDeviceInstanceIn0Xml(deviceAddress, projectRootPath)
      deviceInstanceXMLOpt match {
        case Some(deviceInstanceXML) => {
          val productRefId = deviceInstanceXML \@ PRODUCTREFID_PARAM
          val hardware2programRefId = deviceInstanceXML \@ HARDWARE2PROGRAMREFID_PARAM
          // Find groupObjectInstances id and search them in the files
          val groupObjectTreeInstance = deviceInstanceXML \\ GROUPOBJECTTREE_TAG
          val nodes = groupObjectTreeInstance \\ NODES_TAG
          val nodesChannels = (nodes \\ NODE_TAG).par
            .map(n =>
              ChannelNode(
                constructChannelNodeName(n, productRefId, hardware2programRefId),
                (n \@ GROUPOBJECTINSTANCES_PARAM).split(' ').flatMap(getCommObjectsFromString(etsProjectPath, _, productRefId, hardware2programRefId)).toList
              )
            )
            .toList
          // Now gets the channels that are not in a Node but naked at the root
          val rootChannel = ChannelNode(
            defaultNodeName,
            (groupObjectTreeInstance \@ GROUPOBJECTINSTANCES_PARAM)
              .split(' ')
              .flatMap(getCommObjectsFromString(etsProjectPath, _, productRefId, hardware2programRefId))
              .toList
          )

          if (rootChannel.ioPorts.isEmpty) {
            nodesChannels
          } else {
            nodesChannels ++ List(rootChannel)
          }
        }
        case None => throw new MalformedXMLException("Cannot get the deviceInstance in xml")
      }
    }
  )

  private def getIOPortTypeFromFlags(comObjectNode: Node): String = {
    val cFlag = (comObjectNode \@ COMMUNICATIONFLAG_PARAM) == enabledText // Tells if the device is communicating (i.e., the other flags mean something
    val rFlag = (comObjectNode \@ READFLAG_PARAM) == enabledText
    val tFlag = (comObjectNode \@ TRANSMITFLAG_PARAM) == enabledText
    val wFlag = (comObjectNode \@ WRITEFLAG_PARAM) == enabledText
    val uFlag = (comObjectNode \@ UPDATEFLAG_PARAM) == enabledText
    val iFlag = (comObjectNode \@ READONINITFLAG_PARAM) == enabledText

    val in = wFlag || uFlag
    val out = rFlag || tFlag

//    if(!cFlag) throw new CommunicationFlagDisabledOnCommObject(s"Communication flag disabled for the communication object ${comObjectNode \@ ID_PARAM}")
    if (in && out) InOut.toString
    else if (out) {
      // This device will either write on the bus or respond to read request --> Out
      Out.toString
    } else if (in) {
      // This device will overwrite its value with the one coming from the bus --> In
      In.toString
    } else {
//      throw new NoFlagsOnCommObject(s"No flags set for the communication object ${comObjectNode \@ ID_PARAM}")
      Unknown.toString
    }
  }

  private def getCommObjectsFromString(etsProjectPath: os.Path, groupObjectInstanceId: String, productRefId: String, hardware2ProgramRefId: String): List[CommObject] =
    extractIfNotExist(
      etsProjectPath,
      _ => {
        if (groupObjectInstanceId.nonEmpty) {
          // Get IOPort info in xmls
          val xmlPath = productCatalogXMLFile(etsProjectPath, productRefId, hardware2ProgramRefId)
          val catalogEntry = takeFromCacheOrComputeAndUpdate(xmlPath.toIO)
          val refIdRegEx = "O-[0-9A-Za-z-_]+_R-[0-9A-Za-z-_]+".r
          val refId = refIdRegEx.findAllIn(groupObjectInstanceId).toList.last
          val comObjectRef = (catalogEntry \\ COMOBJECTREF_TAG).par.find(n => (n \@ ID_PARAM).contains(refId))
          comObjectRef match {
            case Some(comObjectRef) => {
              val comObjectRefDPTString = comObjectRef \@ DATAPOINTTYPE_PARAM
              val refId = comObjectRef \@ REFID_PARAM
              val comObject = (catalogEntry \\ COMOBJECT_TAG).par.find(n => (n \@ ID_PARAM) == refId)
              comObject match {
                case Some(value) => {
                  val comObjectDPTString = value \@ DATAPOINTTYPE_PARAM
                  val dptStr = if (comObjectDPTString.nonEmpty) comObjectDPTString else comObjectRefDPTString
                  CommObject(constructCommObjectName(value, comObjectRef, catalogEntry), dptStr, getIOPortTypeFromFlags(value), value \@ OBJECTSIZE_PARAM) :: Nil
                }
                case None => throw new MalformedXMLException(s"Cannot find the ComObject for the id: $refId for the productRefId: $productRefId")
              }
            }
            case None => throw new MalformedXMLException(s"Cannot find the ComObjectRef for the id: $groupObjectInstanceId for the productRefId: $productRefId")
          }

        } else {
          Nil
        }
      }
    )

  private def constructCommObjectName(ioPortNode: Node, comObjectRef: Node, catalogEntry: Elem): String = {
    def validContent(tr: String): Boolean = tr != "" && tr != "-" && !tr.contains("GO_")
    val funTextIoNode = getTranslation(catalogEntry, enUsLanguageCode, ioPortNode \@ ID_PARAM, FUNCTIONTEXT_PARAM) match {
      case Some(value) => if (validContent(value)) value else ioPortNode \@ FUNCTIONTEXT_PARAM
      case None        => ioPortNode \@ FUNCTIONTEXT_PARAM
    }
    val funTextRef = getTranslation(catalogEntry, enUsLanguageCode, comObjectRef \@ ID_PARAM, FUNCTIONTEXT_PARAM) match {
      case Some(value) => if (validContent(value)) value else comObjectRef \@ FUNCTIONTEXT_PARAM
      case None        => comObjectRef \@ FUNCTIONTEXT_PARAM
    }
    val nameTextIoNode = getTranslation(catalogEntry, enUsLanguageCode, ioPortNode \@ ID_PARAM, NAME_PARAM) match {
      case Some(value) => if (validContent(value)) value else ioPortNode \@ NAME_PARAM
      case None        => ioPortNode \@ NAME_PARAM
    }
    val nameTextRef = getTranslation(catalogEntry, enUsLanguageCode, comObjectRef \@ ID_PARAM, NAME_PARAM) match {
      case Some(value) => if (validContent(value)) value else comObjectRef \@ NAME_PARAM
      case None        => comObjectRef \@ NAME_PARAM
    }

    val textTextIoNode = getTranslation(catalogEntry, enUsLanguageCode, ioPortNode \@ ID_PARAM, TEXT_PARAM) match {
      case Some(value) => if (validContent(value)) value else ioPortNode \@ TEXT_PARAM
      case None        => ioPortNode \@ TEXT_PARAM
    }
    val textTextRef = getTranslation(catalogEntry, enUsLanguageCode, comObjectRef \@ ID_PARAM, TEXT_PARAM) match {
      case Some(value) => if (validContent(value)) value else comObjectRef \@ TEXT_PARAM
      case None        => comObjectRef \@ TEXT_PARAM
    }

    List(funTextIoNode, funTextRef, nameTextIoNode, nameTextRef, textTextIoNode, textTextRef).filter(validContent).mkString(" - ")
  }

  /** Search for a translation for the given id and given attribute name, for the given language, if not found, return None
    * @param xmlElem
    * @param languageIdentifier
    * @param refID
    * @param attributeName
    * @return
    */
  private def getTranslation(xmlElem: Elem, languageIdentifier: String, refID: String, attributeName: String): Option[String] = {
    val languageObjectOpt = (xmlElem \\ LANGUAGE_TAG).par.find(n => n \@ IDENTIFIER_PARAM == languageIdentifier)
    if (languageObjectOpt.isDefined) {
      val translationElementOpt = (languageObjectOpt.get \\ TRANSLATIONELEMENT_TAG).par.find(n => (n \@ REFID_PARAM) == refID)
      if (translationElementOpt.isDefined) {
        (translationElementOpt.get \\ TRANSLATION_TAG).par.find(n => n \@ ATTRIBUTENAME_PARAM == attributeName) match {
          case Some(value) => Some(value \@ TEXT_PARAM)
          case None        => None
        }
      } else {
        None
      }
    } else {
      None
    }
  }

  private def hardwareXmlFile(etsProjectPath: os.Path, productRefId: String, hardware2ProgramRefId: String): os.Path = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      val catalogId = productRefId.split('_').apply(0) // e.g., "M-0002"
      val filePathOpt = recursiveListFiles(projectRootPath / catalogId).find(file => file.toIO.getName.contains(HARDWARE_XML_NAME))
      filePathOpt match {
        case Some(value) => value
        case None        => throw new MalformedXMLException(s"Cannot find the hardware.xml file in the folder $catalogId in the project.")
      }
    }
  )

  private def productCatalogXMLFile(etsProjectPath: os.Path, productRefId: String, hardware2ProgramRefId: String): os.Path = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      val catalogId = productRefId.split('_').apply(0) // e.g., "M-0002"
      val productIdPattern = "HP-[0-9A-Z]{4}-[0-9A-Z]{2}-[0-9A-Z]{4}".r
      val productIdOpt = productIdPattern.findFirstIn(hardware2ProgramRefId)
      productIdOpt match {
        case Some(productId) => {
          val filePathOpt = recursiveListFiles(projectRootPath / catalogId).find(file => file.toIO.getName.contains(productId.replaceFirst("HP-", "")))
          filePathOpt match {
            case Some(value) => value
            case None        => throw new MalformedXMLException(s"Cannot find the file for productId = $productId in the folder $catalogId in the project.")
          }
        }
        case None => {
          val patternString = "HP-[0-9A-Z]{4}-[0-9A-Z]{2}-[0-9A-Z]{4}"
          throw new MalformedXMLException(s"Cannot find the correct id matching $patternString in the hardware2programRefId = $hardware2ProgramRefId")
        }
      }
    }
  )

  /** Explore the 0.xml file and returns the list of devices addresses as tuples
    * e.g., (1, 1, 1), (areaN, lineN, deviceN)
    *
    * @param etsProjectFilePath : the path to the file of etsProject as os.Path
    * @return
    */
  private def explore0xmlFindListAddresses(etsProjectPath: os.Path): List[(String, String, String)] = extractIfNotExist(
    etsProjectPath,
    projectRootPath => {
      val file0XmlPath = recursiveListFiles(projectRootPath).find(file => file.toIO.getName == FILE_0_XML_NAME)
      if (file0XmlPath.isEmpty) throw new MalformedXMLException("Missing 0.xml")
      val doc0xml = takeFromCacheOrComputeAndUpdate(file0XmlPath.get.toIO)
      val areasXml = (doc0xml \\ AREA_TAG)
      areasXml.par
        .flatMap(area =>
          (area \\ LINE_TAG).flatMap(line => (line \\ DEVICE_TAG).flatMap(deviceInstance => (area \@ ADDRESS_PARAM, line \@ ADDRESS_PARAM, deviceInstance \@ ADDRESS_PARAM) :: Nil))
        )
        .filter { case (areaId, lineId, deviceId) =>
          areaId.nonEmpty && lineId.nonEmpty && deviceId.nonEmpty
        }
        .toList
    }
  )

  private def extractIfNotExist[B](etsProjectPath: os.Path, operation: os.Path => B): B = {
    val extractedPath = computeExtractedPath(etsProjectPath)
    val unzippedPath = if (!os.exists(extractedPath)) unzip(etsProjectPath, extractedPath) else Some(extractedPath)
    unzippedPath match {
      case Some(projectRootPath) => operation(projectRootPath)
      case None                  => throw new FileNotFoundException()
    }
  }

  /** Compute the Path of the temporary location where the project is unzipped
    * @param etsProjectPath
    * @tparam B
    * @return
    */
  def computeExtractedPath[B](etsProjectPath: os.Path): os.Path = {
    tempFolderPath / etsProjectPath.segments.toList.last.appendedAll(unzippedSuffix)
  }

  private def deleteUnzippedFiles(etsProjectPath: os.Path): Unit = {
    val extractedPath = computeExtractedPath(etsProjectPath)
    deleteRecursive(extractedPath)
  }

  private def deleteRecursive(filePath: os.Path): Unit = {
    if (os.isDir(filePath)) {
      if (os.list(filePath).toList.nonEmpty) {
        os.list(filePath).toList.foreach(file => deleteRecursive(file))
      }
      os.remove.all(filePath)
    } else {
      os.remove.all(filePath)
    }
  }

  class MalformedXMLException(msg: String) extends Exception(msg)
  class UnsupportedDatatype(msg: String) extends Exception(msg)
  class CommunicationFlagDisabledOnCommObject(msg: String) extends Exception(msg)
  class NoFlagsOnCommObject(msg: String) extends Exception(msg)
}
