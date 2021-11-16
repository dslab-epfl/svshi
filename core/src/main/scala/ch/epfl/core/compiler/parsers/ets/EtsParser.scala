package ch.epfl.core.compiler.parsers.ets

import ch.epfl.core.models.physical
import ch.epfl.core.models.physical._
import ch.epfl.core.utils.FileUtils._

import java.io.FileNotFoundException
import java.nio.file.{Files, Path}
import scala.util.matching.Regex
import scala.xml.{Node, XML}

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

  val enabledText = "Enabled"
  val disabledText = "Disabled"

  val etsDpstRegex: Regex = "DPST-[0-9]+".r
  val etsDptRegex: Regex = "DPT-[0-9]+".r

  private val tempFolderPath = Path.of("temp")
  private val defaultNodeName = "Default"


  def parseEtsProjectFile(etsProjectPathString: String) : PhysicalStructure = extractIfNotExist(etsProjectPathString, projectRootPath =>{
    val deviceAddresses = explore0xmlFindListAddresses(etsProjectPathString)
    val parsedDevices = deviceAddresses.map(readDeviceFromEtsFile(etsProjectPathString,  _))
    physical.PhysicalStructure(parsedDevices.map(parsedDeviceToPhysicalDevice))
  })

  private def parsedDeviceToPhysicalDevice(parsedDevice: ParsedDevice): PhysicalDevice = {
    /**
     * DPT types can be either format DPST-X-Y or DPT-X
     * The id is the hash using MurmurHash3 on List(name, datatype, ioType)
     * @param parsedioPort
     * @return
     */
    def ioPortToPhysicalChannel(parsedioPort: IOPort): PhysicalDeviceCommObject = {
      val dpstOpt = etsDpstRegex.findFirstIn(parsedioPort.dpt)
      val dptOpt = etsDptRegex.findFirstIn(parsedioPort.dpt)
      val datatype = if(dpstOpt.isDefined)
        KNXDatatype.fromString(dpstOpt.get.replace("S", ""))
      else if(dptOpt.isDefined) KNXDatatype.fromString(dptOpt.get)
      else if(KNXDatatype.fromDPTSize(parsedioPort.objectSizeString).isDefined) KNXDatatype.fromDPTSize(parsedioPort.objectSizeString)
      else if(parsedioPort.dpt == "") Some(UnknownDPT)
      else throw new MalformedXMLException(s"The DPT is not formatted as $etsDptRegex or $etsDpstRegex (or empty String) and the ObjectSize is not convertible to DPT (objectSize = ${parsedioPort.objectSizeString}) for the IOPort $parsedioPort for the device with address ${parsedDevice.address}")
      if(datatype.isEmpty) throw new UnsupportedDatatype(s"The Datatype $parsedioPort.dpt is not supported")
      val ioType = IOType.fromString(parsedioPort.inOutType).get
      PhysicalDeviceCommObject.from(parsedioPort.name, datatype.get, ioType)
    }
    physical.PhysicalDevice(parsedDevice.name, parsedDevice.address, parsedDevice.io.map(parsedNode => PhysicalDeviceNode(parsedNode.name, parsedNode.ioPorts.map(ioPortToPhysicalChannel))))
  }
  /**
   * Reads one device from the ETS xml project
   *
   * It assumes that the project contains only one installation
   *
   * @param etsProjectPathString the path to the etsProject file as String
   * @param deviceAddress  the address of the device in topology of the project e.g., 1.1.1
   */
  private def readDeviceFromEtsFile(etsProjectPathString: String, deviceAddress: (String, String, String)): ParsedDevice = extractIfNotExist(etsProjectPathString, projectRootPath => {
    val deviceInstanceXMLOpt: Option[Node] = getDeviceInstanceIn0Xml(deviceAddress, projectRootPath)
    deviceInstanceXMLOpt match {
      case Some(deviceInstanceXML) => {
        val productRefId = deviceInstanceXML \@ PRODUCTREFID_PARAM
        val deviceName = getDeviceNameInCatalog(etsProjectPathString, productRefId)
        val inOut = getDeviceInOutInCatalog(etsProjectPathString, deviceAddress)
        ParsedDevice(deviceAddress, deviceName, inOut)
      }
      case None => throw new MalformedXMLException(s"Cannot find the XML specific to $deviceAddress in $etsProjectPathString")
    }
  })

  private def getDeviceInstanceIn0Xml(deviceAddress: (String, String, String), projectRootPath: Path): Option[Node] = {
    val file0XmlPath = recursiveListFiles(projectRootPath.toFile).find(file => file.getName == FILE_0_XML_NAME)
    if (file0XmlPath.isEmpty) throw new MalformedXMLException("Missing 0.xml")
    val (areaN, lineN, deviceN) = deviceAddress
    val doc0xml = XML.loadFile(file0XmlPath.get)
    val deviceInstanceXMLOpt = (((doc0xml \\ AREA_TAG).find(a => a \@ ADDRESS_PARAM == areaN).get \\ LINE_TAG).find(l => l \@ ADDRESS_PARAM == lineN).get \\ DEVICE_TAG).find(d => d \@ ADDRESS_PARAM == deviceN)
    deviceInstanceXMLOpt
  }

  /**
   * Get the name of the device in the Hardware.xml file in the ETS project
   *
   * @param etsProjectPathString the path to the etsProject file as String
   * @param productRefId   the productRefId of the device
   * @return
   */
  private def getDeviceNameInCatalog(etsProjectPathString: String, productRefId: String): String = extractIfNotExist(etsProjectPathString, projectRootPath => {
    val catalogID = productRefId.split('_').apply(0)
    val folderHardwareXmlPath = recursiveListFiles(projectRootPath.toFile).find(file => file.getName == catalogID)
    if (folderHardwareXmlPath.isEmpty || folderHardwareXmlPath.get.isFile) throw new MalformedXMLException(s"Missing folder for product with catalogID = $catalogID")
    val fileHardwareXmlPath = recursiveListFiles(folderHardwareXmlPath.get).find(file => file.getName == HARDWARE_XML_NAME)
    if (fileHardwareXmlPath.isEmpty) throw new MalformedXMLException(s"Missing Hardware.xml for product with catalogID = $catalogID")
    val hardwareXml = XML.loadFile(fileHardwareXmlPath.get)
    val productXml = (hardwareXml \\ PRODUCT_TAG).find(p => p \@ ID_PARAM == productRefId)
    if (productXml.isEmpty) throw new MalformedXMLException(s"Impossible to find the product name in the project for productRefID = $productRefId")
    productXml.get \@ TEXT_PARAM
  })

  def getDeviceInOutInCatalog(etsProjectPathString: String, deviceAddress: (String, String, String)): List[ChannelNode] = extractIfNotExist(etsProjectPathString, projectRootPath => {
    def constructChannelNodeName(n: Node) ={
      val typeText = n \@ TYPE_PARAM
      val refIdText = n \@ REFID_PARAM
      val textText =  n \@ TEXT_PARAM
      List(typeText, refIdText, textText).filterNot(_ == "").mkString(" - ")
    }

    val deviceInstanceXMLOpt: Option[Node] = getDeviceInstanceIn0Xml(deviceAddress, projectRootPath)
    deviceInstanceXMLOpt match {
      case Some(deviceInstanceXML) => {
        val productRefId = deviceInstanceXML \@ PRODUCTREFID_PARAM
        val hardware2programRefId = deviceInstanceXML \@ HARDWARE2PROGRAMREFID_PARAM
        // Find groupObjectInstances id and search them in the files
        val groupObjectTreeInstance = deviceInstanceXML \\ GROUPOBJECTTREE_TAG
        val nodes = groupObjectTreeInstance \\ NODES_TAG
        if (nodes.isEmpty) {
          // There is no Nodes object in the xml, but only directly the GroupObjectInstances
          ChannelNode(defaultNodeName, (groupObjectTreeInstance \@ GROUPOBJECTINSTANCES_PARAM).split(' ').flatMap(getIOPortFromString(etsProjectPathString, _, productRefId, hardware2programRefId)).toList) :: Nil
        } else {
          (nodes \\ NODE_TAG).map(n => ChannelNode(constructChannelNodeName(n), (n \@ GROUPOBJECTINSTANCES_PARAM).split(' ').flatMap(getIOPortFromString(etsProjectPathString, _, productRefId, hardware2programRefId)).toList)).toList
        }

      }
      case None => throw new MalformedXMLException("Cannot get the deviceInstance in xml")
    }
  })

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
    if(in && out) InOut.toString
    else if(out){
      // This device will either write on the bus or respond to read request --> Out
      Out.toString
    } else if(in){
      // This device will overwrite its value with the one coming from the bus --> In
      In.toString
    } else {
//      throw new NoFlagsOnCommObject(s"No flags set for the communication object ${comObjectNode \@ ID_PARAM}")
      Unknown.toString
    }
  }

  private def getIOPortFromString(etsProjectPathString: String, groupObjectInstanceId: String, productRefId: String, hardware2ProgramRefId: String): List[IOPort] = extractIfNotExist(etsProjectPathString, projectRootPath => {
    if (groupObjectInstanceId.nonEmpty) {
      // Get IOPort info in xmls
      val xmlPath = productCatalogXMLFile(etsProjectPathString, productRefId, hardware2ProgramRefId)
      val catalogEntry = XML.loadFile(xmlPath.toFile)
      val comObjectRef = (catalogEntry \\ COMOBJECTREF_TAG).find(n => (n \@ ID_PARAM).contains(groupObjectInstanceId))
      comObjectRef match {
        case Some(comObjectRef) => {
          val refId = (comObjectRef \@ REFID_PARAM)
          val comObject = (catalogEntry \\ COMOBJECT_TAG).find(n => (n \@ ID_PARAM) == refId)
          comObject match {
            case Some(value) =>  IOPort(constructIOPortName(value), value \@ DATAPOINTTYPE_PARAM, getIOPortTypeFromFlags(value), value \@ OBJECTSIZE_PARAM ) :: Nil
            case None => throw new MalformedXMLException(s"Cannot find the ComObject for the id: $refId for the productRefId: $productRefId")
          }
        }
        case None => throw new MalformedXMLException(s"Cannot find the ComObjectRef for the id: $groupObjectInstanceId for the productRefId: $productRefId")
      }

    } else {
      Nil
    }
  })

  private def constructIOPortName(ioPortNode: Node) : String = {
    val funText = ioPortNode \@ FUNCTIONTEXT_PARAM
    val nameText = ioPortNode \@ NAME_PARAM
    val textText = ioPortNode \@ TEXT_PARAM
    List(funText, nameText, textText).filterNot(_ == "").mkString(" - ")
  }

  private def productCatalogXMLFile(etsProjectPathString: String, productRefId: String, hardware2ProgramRefId: String): Path = extractIfNotExist(etsProjectPathString, projectRootPath => {
    val catalogId = productRefId.split('_').apply(0) // e.g., "M-0002"
    val productIdPattern = "HP-[0-9A-Z]{4}-[0-9A-Z]{2}-[0-9A-Z]{4}".r
    val productIdOpt = productIdPattern.findFirstIn(hardware2ProgramRefId)
    productIdOpt match {
      case Some(productId) => {
        val filePathOpt = recursiveListFiles(projectRootPath.resolve(catalogId).toFile).find(file => file.getName.contains(productId.replaceFirst("HP-", "")))
        filePathOpt match {
          case Some(value) => value.toPath
          case None => throw new MalformedXMLException(s"Cannot find the file for productId = $productId in the folder $catalogId in the project.")
        }
      }
      case None => {
        val patternString = "HP-[0-9A-Z]{4}-[0-9A-Z]{2}-[0-9A-Z]{4}"
        throw new MalformedXMLException(s"Cannot find the correct id matching $patternString in the hardware2programRefId = $hardware2ProgramRefId")
      }
    }
  })

  /**
   * Explore the 0.xml file and returns the list of devices addresses as tuples
   * e.g., (1, 1, 1), (areaN, lineN, deviceN)
   *
   * @param etsProjectFilePathString : the path to the file of etsProject as String
   * @return
   */
  def explore0xmlFindListAddresses(etsProjectFilePathString: String): List[(String, String, String)] = extractIfNotExist(etsProjectFilePathString, projectRootPath => {
    val file0XmlPath = recursiveListFiles(projectRootPath.toFile).find(file => file.getName == FILE_0_XML_NAME)
    if (file0XmlPath.isEmpty) throw new MalformedXMLException("Missing 0.xml")
    val doc0xml = XML.loadFile(file0XmlPath.get)
    val areasXml = (doc0xml \\ AREA_TAG)
    areasXml.flatMap(area =>
      (area \\ LINE_TAG).flatMap(line =>
        (line \\ DEVICE_TAG).flatMap(deviceInstance =>
          (area \@ ADDRESS_PARAM, line \@ ADDRESS_PARAM, deviceInstance \@ ADDRESS_PARAM) :: Nil))).toList
  })

  private def extractIfNotExist[B](etsProjectPathString: String, operation: Path => B): B = {
    val extractedPath = Path.of(tempFolderPath.resolve(Path.of(etsProjectPathString).getFileName).toUri.toString.appendedAll(unzippedSuffix))
    val unzippedPath = if (!Files.exists(extractedPath)) unzip(etsProjectPathString, extractedPath.toString) else Some(extractedPath)
    unzippedPath match {
      case Some(projectRootPath) => operation(projectRootPath)
      case None => throw new FileNotFoundException()
    }
  }

  class MalformedXMLException(msg: String) extends Exception(msg)
  class UnsupportedDatatype(msg: String) extends Exception(msg)
  class CommunicationFlagDisabledOnCommObject(msg: String) extends Exception(msg)
  class NoFlagsOnCommObject(msg: String) extends Exception(msg)
}
