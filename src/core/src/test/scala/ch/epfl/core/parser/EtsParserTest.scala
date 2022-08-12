package ch.epfl.core.parser

import ch.epfl.core.model.physical._
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.utils.Constants
import ch.epfl.core.utils.Constants.SVSHI_HOME_PATH
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EtsParserTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  val testFilePathString1 = s"${Constants.SVSHI_SRC_FOLDER}/core/res/ets_project_test.knxproj"
  val testFilePath1: os.Path = os.Path(testFilePathString1, SVSHI_HOME_PATH)
  val testFilePathString2 = s"${Constants.SVSHI_SRC_FOLDER}/core/res/DSLAB_proto.knxproj"
  val testFilePath2: os.Path = os.Path(testFilePathString2, SVSHI_HOME_PATH)
  val testFilePathString3 = s"${Constants.SVSHI_SRC_FOLDER}/core/res/SVSHI_Virtual.knxproj"
  val testFilePath3: os.Path = os.Path(testFilePathString3, SVSHI_HOME_PATH)

  override def beforeEach(): Unit = {
    if (os.exists(Constants.SVSHI_ETS_PARSER_TEMP_FOLDER_PATH)) os.remove.all(Constants.SVSHI_ETS_PARSER_TEMP_FOLDER_PATH)
  }
  override def afterEach(): Unit = {
    if (os.exists(Constants.SVSHI_ETS_PARSER_TEMP_FOLDER_PATH)) os.remove.all(Constants.SVSHI_ETS_PARSER_TEMP_FOLDER_PATH)
  }
  "parseEtsProjectFile" should "return the correct structure on the test file 1" in {
    val structure = EtsParser.parseEtsProjectFile(testFilePath1)
    val device1 = PhysicalDevice(
      name = "SA/S2.16.2.2 Switch Act, 2f, 16A, MDRC",
      address = ("1", "1", "1"),
      nodes = List(
        PhysicalDeviceNode("Channel - CH-1 - Device settings", Nil),
        PhysicalDeviceNode("Channel - CH-2 - Safety", Nil),
        PhysicalDeviceNode("Channel - CH-3 - Logic/threshold", Nil),
        PhysicalDeviceNode("Channel - CH-4 - Switch actuator template", Nil),
        PhysicalDeviceNode(
          "Channel - CH-5 - Switch actuator A",
          List(
            PhysicalDeviceCommObject.from(
              "Channel A: Switch - Obj_Switching - Switch",
              DPT1(1),
              In,
              ("1", "1", "1"),
              "Channel - CH-5 - Switch actuator A"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Switch actuator B",
          List(
            PhysicalDeviceCommObject.from(
              "Channel B: Switch - Obj_Switching - Switch",
              DPT1(1),
              In,
              ("1", "1", "1"),
              "Channel - CH-6 - Switch actuator B"
            )
          )
        )
      )
    )

    val device2 =
      PhysicalDevice(
        name = "STANDARDdue Push-button moisture protection",
        address = ("1", "1", "2"),
        nodes = List(
          PhysicalDeviceNode(
            "Default",
            List(PhysicalDeviceCommObject.from("Object 1 - Object 1 - Push-button - ON/OFF, switching", DPT1(1), InOut, ("1", "1", "2"), "Default"))
          )
        )
      )

    val device3 = PhysicalDevice(
      name = "SBR/U6.0 HVAC device, 6gang BE",
      address = ("1", "1", "3"),
      nodes = List(
        PhysicalDeviceNode("Channel - CH-1 - Device settings", List()),
        PhysicalDeviceNode("Channel - CH-2 - Primary function", List()),
        PhysicalDeviceNode(
          "Channel - CH-3 - RTC",
          List(
            PhysicalDeviceCommObject.from(
              "RTC: Heating control value - Obj_StellgroesseHeizen_Switch - Output",
              DPT1(1),
              Out,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "RTC: Normal operating mode - Obj_Betriebsart_Single - Input/Output",
              DPT20(102),
              InOut,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "RTC: Override operating mode - Obj_BetriebsartUeberlagert_Single - Input",
              DPT20(102),
              InOut,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "RTC: Actual temperature - Obj_AktuelleIstTemperatur - Output",
              DPT9(1),
              Out,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "RTC: actual setpoint - Obj_AktuellerSollwert - Output",
              DPT9(1),
              Out,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-4 - Function block 1",
          List(
            PhysicalDeviceCommObject.from(
              "S1: Switching - Obj_Wert1Bit - Input/Output",
              DPT1(1),
              InOut,
              ("1", "1", "3"),
              "Channel - CH-4 - Function block 1"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-5 - Function block 2",
          List(
            PhysicalDeviceCommObject.from(
              "S3: Switching - Obj_Wert1Bit - Input/Output",
              DPT1(1),
              InOut,
              ("1", "1", "3"),
              "Channel - CH-5 - Function block 2"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Function block 3",
          List(
            PhysicalDeviceCommObject.from(
              "S5: Switching - Obj_Wert1Bit - Input/Output",
              DPT1(1),
              InOut,
              ("1", "1", "3"),
              "Channel - CH-6 - Function block 3"
            )
          )
        ),
        PhysicalDeviceNode("Channel - CH-7 - Common functions", List())
      )
    )

    val device4 = PhysicalDevice(
      name = "AC/S1.2.1 Application Controller,BACnet",
      address = ("1", "1", "4"),
      nodes = List(
        PhysicalDeviceNode(
          "Channel - CH-1 - Device clock",
          List(
            PhysicalDeviceCommObject.from("Device clock - Request time", DPT1(17), Unknown, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Device clock - Date", DPT11(1), Out, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Device clock - Time", DPT10(1), Out, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Device clock - Date/Time", DPT19(1), Out, ("1", "1", "4"), "Channel - CH-1 - Device clock")
          )
        )
      )
    )

    val device5 = PhysicalDevice(
      name = "Valve actuator",
      address = ("1", "2", "1"),
      nodes = List(
        PhysicalDeviceNode(
          "Default",
          List(
            PhysicalDeviceCommObject.from(
              "Valve - Vanne - Command %",
              DPTUnknown(-1),
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Valve - Vanne - Priority %",
              DPT1(-1),
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from("Valve - Vanne - Stop", DPT1(-1), InOut, ("1", "2", "1"), "Default"),
            PhysicalDeviceCommObject.from(
              "Status indication - Indication d'état - Valve position %",
              DPTUnknown(-1),
              Out,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Status indication - Indication d'état - Highest command value",
              DPTUnknown(-1),
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Status indication - Indication d'état - Presence / Absence command",
              DPT1(-1),
              Out,
              ("1", "2", "1"),
              "Default"
            )
          )
        )
      )
    )

    structure.deviceInstances.exists(p => p.address == device1.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device1.address) shouldEqual Some(device1)
    structure.deviceInstances.exists(p => p.address == device2.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device2.address) shouldEqual Some(device2)
    structure.deviceInstances.exists(p => p.address == device3.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device3.address) shouldEqual Some(device3)
    structure.deviceInstances.exists(p => p.address == device4.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device4.address) shouldEqual Some(device4)
    structure.deviceInstances.exists(p => p.address == device5.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device5.address) shouldEqual Some(device5)
  }

  "parseEtsProjectFile" should "have deleted temporary unzipped file when it is done" in {
    EtsParser.parseEtsProjectFile(testFilePath1)
    os.exists(EtsParser.computeExtractedPath(testFilePath1)) shouldBe false
  }

  "parseEtsProjectFile" should "return the correct structure on the test file 2" in {
    val structure = EtsParser.parseEtsProjectFile(testFilePath2)

    val device1 =
      PhysicalDevice(name = "IPS/S3.1.1 IP Interface,MDRC", address = ("1", "1", "1"), nodes = List(PhysicalDeviceNode(name = "Channel - CH-0 - IP settings", comObjects = List())))

    val device2 = PhysicalDevice(
      name = "US/U2.2 Universal Interface,2-fold,FM",
      address = ("1", "1", "7"),
      nodes = List(
        PhysicalDeviceNode(
          name = "Default",
          comObjects = List(
            PhysicalDeviceCommObject.from(name = "Input A - Eingang A - Disable", datatype = DPT1(-1), ioType = In, physicalAddress = ("1", "1", "7"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject
              .from(
                name = "Input A - Eingang A - Telegr. switch - Telegr. counter value 2 bytes",
                datatype = DPT1(-1),
                ioType = InOut,
                physicalAddress = ("1", "1", "7"),
                deviceNodeName = "Default"
              ),
            PhysicalDeviceCommObject.from(name = "Input B - Eingang B - Disable", datatype = DPT1(-1), ioType = In, physicalAddress = ("1", "1", "7"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(
              name = "Output B - Ausgang B - Input B - Telegr. switch - Control value (PWM)",
              datatype = DPT1(-1),
              ioType = InOut,
              physicalAddress = ("1", "1", "7"),
              deviceNodeName = "Default"
            )
          )
        )
      )
    )

    val device3 = PhysicalDevice(
      name = "LUXORliving S1",
      address = ("1", "1", "8"),
      nodes = List(
        PhysicalDeviceNode(
          name = "Channel - CH-1 - Channel C1",
          comObjects = List(
            PhysicalDeviceCommObject.from(
              name = "Channel C1 - Switch object - Threshold as a percentage",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "8"),
              deviceNodeName = "Channel - CH-1 - Channel C1"
            ),
            PhysicalDeviceCommObject
              .from(name = "Channel C1 - Feedback On/Off", datatype = DPT1(1), ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-1 - Channel C1")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-2 - Input I1",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Channel I1 - Switching", datatype = DPT1(1), ioType = InOut, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-2 - Input I1")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-3 - Input I2",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Channel I2 - Switching", datatype = DPT1(1), ioType = InOut, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-3 - Input I2")
          )
        ),
        PhysicalDeviceNode(
          name = "Default",
          comObjects = List(
            PhysicalDeviceCommObject.from(name = "Alarm - Excess temperature", datatype = DPT1(5), ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "Central permanent - ON", datatype = DPT1(1), ioType = In, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "Central switching - OFF", datatype = DPT1(1), ioType = In, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default")
          )
        )
      )
    )

    val device4 = PhysicalDevice(
      name = "Set basic KNX Multi",
      address = ("1", "1", "10"),
      nodes = List(
        PhysicalDeviceNode(
          name = "Channel - CH-0 - General",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Firmware - Version", datatype = DPTUnknown(-1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "CO2 value - Send", datatype = DPT9(8), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Relative humidity - Send", datatype = DPT9(7), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Air pressure - Send", datatype = DPT14(58), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Degree of comfort - Send", datatype = DPT5(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Temperature value - Send", datatype = DPT9(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject.from(
              name = "CO2 Offset - Measurement value offset",
              datatype = DPT9(-1),
              ioType = InOut,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-0 - General"
            ),
            PhysicalDeviceCommObject.from(
              name = "CO2 reference - Receive measurement value",
              datatype = DPT9(8),
              ioType = InOut,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-0 - General"
            )
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-2 - CO2 sensor",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(
                name = "CO2 threshold 1 - Switching",
                datatype = DPT1(1),
                ioType = Out,
                physicalAddress = ("1", "1", "10"),
                deviceNodeName = "Channel - CH-2 - CO2 sensor"
              ),
            PhysicalDeviceCommObject
              .from(
                name = "CO2 threshold 2 - Switching",
                datatype = DPT1(1),
                ioType = Out,
                physicalAddress = ("1", "1", "10"),
                deviceNodeName = "Channel - CH-2 - CO2 sensor"
              ),
            PhysicalDeviceCommObject
              .from(
                name = "CO2 threshold 3 - Switching",
                datatype = DPT1(1),
                ioType = Out,
                physicalAddress = ("1", "1", "10"),
                deviceNodeName = "Channel - CH-2 - CO2 sensor"
              ),
            PhysicalDeviceCommObject.from(
              name = "Ventilation of CO2 - Actuating value 0-100%",
              datatype = DPT5(1),
              ioType = Out,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-2 - CO2 sensor"
            ),
            PhysicalDeviceCommObject
              .from(name = "CO2 scenes - Send", datatype = DPT17(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-2 - CO2 sensor")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-3 - Humidity sensor",
          comObjects = List(
            PhysicalDeviceCommObject.from(
              name = "Humidity threshold 1 - Switching",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Humidity threshold 2 - Switching",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Humidity threshold 3 - Switching",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Ventilating humidity - Actuating value 0-100%",
              datatype = DPT5(1),
              ioType = Out,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject
              .from(
                name = "Humidity scenes - Send",
                datatype = DPT17(1),
                ioType = Out,
                physicalAddress = ("1", "1", "10"),
                deviceNodeName = "Channel - CH-3 - Humidity sensor"
              )
          )
        ),
//        PhysicalDeviceNode(
//          name = "Channel - CH-4 - RTR",
//          comObjects = List(
//            PhysicalDeviceCommObject
//              .from(name = "Send - Control actual value", datatype = DPT9(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject
//              .from(name = "Receive - Operating mode preset", datatype = DPT20(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject
//              .from(name = "Receive - Presence", datatype = DPT1(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject
//              .from(name = "Closed=0, open=1 - Window status", datatype = DPT1(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject.from(
//              name = "Save/call up - Operating mode as scene",
//              datatype = DPT18(-1),
//              ioType = Unknown,
//              physicalAddress = ("1", "1", "10"),
//              deviceNodeName = "Channel - CH-4 - RTR"
//            ),
//            PhysicalDeviceCommObject
//              .from(name = "Send - Current operating mode", datatype = DPT20(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject
//              .from(name = "Send - Heating actuating value", datatype = DPT5(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
//            PhysicalDeviceCommObject.from(
//              name = "Defining the set temperature - Base setpoint",
//              datatype = DPT9(-1),
//              ioType = Unknown,
//              physicalAddress = ("1", "1", "10"),
//              deviceNodeName = "Channel - CH-4 - RTR"
//            ),
//            PhysicalDeviceCommObject.from(
//              name = "Send - Setpoint offset at rotary control",
//              datatype = DPT9(-1),
//              ioType = Unknown,
//              physicalAddress = ("1", "1", "10"),
//              deviceNodeName = "Channel - CH-4 - RTR"
//            ),
//            PhysicalDeviceCommObject
//              .from(name = "Setting/sending - Current setpoint", datatype = DPT9(-1), ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR")
//          )
//        ),
        PhysicalDeviceNode(name = "Channel - CH-6 - External inputs", comObjects = List()),
        PhysicalDeviceNode(
          name = "Channel - CH-7 - Comparator",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Comparator - Input 1", datatype = DPT5(1), ioType = In, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-7 - Comparator"),
            PhysicalDeviceCommObject
              .from(name = "Comparator - Output", datatype = DPT5(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-7 - Comparator")
          )
        ),
        PhysicalDeviceNode(
          name = "Default",
          comObjects = List(
            PhysicalDeviceCommObject.from(name = "Alarm - Info", datatype = DPTUnknown(-1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "Alarm - Error text", datatype = DPT16(0), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Default")
          )
        )
      )
    )

    structure.deviceInstances.exists(p => p.address == device1.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device1.address) shouldEqual Some(device1)
    structure.deviceInstances.exists(p => p.address == device2.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device2.address) shouldEqual Some(device2)
    structure.deviceInstances.exists(p => p.address == device3.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device3.address) shouldEqual Some(device3)
    structure.deviceInstances.exists(p => p.address == device4.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device4.address) shouldEqual Some(device4)
  }

  "parseEtsProjectFile" should "return the correct structure on the test file 3" in {
    val structure = EtsParser.parseEtsProjectFile(testFilePath3)

    val device1 = PhysicalDevice(
      "KliX (D4)",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "Channel - MD-2_M-1_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-1_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-2_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-2_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-3_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-3_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-4_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-4_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-5_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-5_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-6_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-6_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-7_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-7_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-2_M-8_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} - Switching : OnOff",
              datatype = DPT1(1),
              ioType = Out,
              physicalAddress = ("1", "1", "1"),
              deviceNodeName = "Channel - MD-2_M-8_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        )
      )
    )
    val device2 = PhysicalDevice(
      "Switching Actuator (D7)",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "Channel - MD-1_M-1_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-1_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-2_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-2_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-3_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-3_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-4_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-4_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-5_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-5_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-6_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-6_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-7_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-7_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - MD-1_M-8_MI-1_CH-argCH - CH-{{argCH}}",
          List(
            PhysicalDeviceCommObject.from(
              name = "CH-{{argCH}} : OnOff",
              datatype = DPT1(1),
              ioType = In,
              physicalAddress = ("1", "1", "2"),
              deviceNodeName = "Channel - MD-1_M-8_MI-1_CH-argCH - CH-{{argCH}}"
            )
          )
        )
      )
    )

    structure.deviceInstances.exists(p => p.address == device1.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device1.address) shouldEqual Some(device1)
    structure.deviceInstances.exists(p => p.address == device2.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device2.address) shouldEqual Some(device2)
  }
}
