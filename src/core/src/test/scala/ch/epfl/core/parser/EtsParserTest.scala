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
              "Switch - Obj_Switching - Channel A: Switch",
              DPT1,
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
              "Switch - Obj_Switching - Channel B: Switch",
              DPT1,
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
            List(PhysicalDeviceCommObject.from("ON/OFF, switching - Object 1 - Object 1 - Push-button", DPT5, InOut, ("1", "1", "2"), "Default"))
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
              "Output - Obj_StellgroesseHeizen_Switch - RTC: Heating control value",
              DPT5,
              Out,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Betriebsart_Single - RTC: Normal operating mode",
              DPT20,
              InOut,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "Input - Obj_BetriebsartUeberlagert_Single - RTC: Override operating mode",
              DPT20,
              InOut,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuelleIstTemperatur - RTC: Actual temperature",
              DPT9,
              Out,
              ("1", "1", "3"),
              "Channel - CH-3 - RTC"
            ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuellerSollwert - RTC: actual setpoint",
              DPT9,
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
              "Input/Output - Obj_Wert1Bit - S1: Switching",
              DPT14,
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
              "Input/Output - Obj_Wert1Bit - S3: Switching",
              DPT14,
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
              "Input/Output - Obj_Wert1Bit - S5: Switching",
              DPT14,
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
            PhysicalDeviceCommObject.from("Request time - Device clock", DPT1, Unknown, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Date - Device clock", DPT11, Out, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Time - Device clock", DPT10, Out, ("1", "1", "4"), "Channel - CH-1 - Device clock"),
            PhysicalDeviceCommObject.from("Date/Time - Device clock", DPT19, Out, ("1", "1", "4"), "Channel - CH-1 - Device clock")
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
              "Command % - Vanne - Valve",
              DPTUnknown,
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Priority % - Vanne - Valve",
              DPT1,
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from("Stop - Vanne - Valve", DPT1, InOut, ("1", "2", "1"), "Default"),
            PhysicalDeviceCommObject.from(
              "Valve position % - Indication d'état - Status indication",
              DPTUnknown,
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Highest command value - Indication d'état - Status indication",
              DPTUnknown,
              InOut,
              ("1", "2", "1"),
              "Default"
            ),
            PhysicalDeviceCommObject.from(
              "Presence / Absence command - Indication d'état - Status indication",
              DPT1,
              InOut,
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
            PhysicalDeviceCommObject.from(name = "Disable - Eingang A - Input A", datatype = DPT1, ioType = In, physicalAddress = ("1", "1", "7"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject
              .from(
                name = "Telegr. counter value 2 bytes - Telegr. switch - Eingang A - Input A",
                datatype = DPTUnknown,
                ioType = InOut,
                physicalAddress = ("1", "1", "7"),
                deviceNodeName = "Default"
              ),
            PhysicalDeviceCommObject.from(name = "Disable - Eingang B - Input B", datatype = DPT1, ioType = In, physicalAddress = ("1", "1", "7"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(
              name = "Control value (PWM) - Telegr. switch - Ausgang B - Output B - Input B",
              datatype = DPTUnknown,
              ioType = In,
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
              name = "Threshold as a percentage - Switch object - Channel C1",
              datatype = DPT1,
              ioType = In,
              physicalAddress = ("1", "1", "8"),
              deviceNodeName = "Channel - CH-1 - Channel C1"
            ),
            PhysicalDeviceCommObject
              .from(name = "Feedback On/Off - Channel C1", datatype = DPT1, ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-1 - Channel C1")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-2 - Input I1",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Switching - Channel I1", datatype = DPT1, ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-2 - Input I1")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-3 - Input I2",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Switching - Channel I2", datatype = DPT1, ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Channel - CH-3 - Input I2")
          )
        ),
        PhysicalDeviceNode(
          name = "Default",
          comObjects = List(
            PhysicalDeviceCommObject.from(name = "Excess temperature - Alarm", datatype = DPT1, ioType = Out, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "ON - Central permanent", datatype = DPT1, ioType = In, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "OFF - Central switching", datatype = DPT1, ioType = In, physicalAddress = ("1", "1", "8"), deviceNodeName = "Default")
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
              .from(name = "Version - Firmware", datatype = DPTUnknown, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Send - CO2 value", datatype = DPT9, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Send - Relative humidity", datatype = DPT9, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Send - Air pressure", datatype = DPT14, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Send - Degree of comfort", datatype = DPT5, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject
              .from(name = "Send - Temperature value", datatype = DPT9, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
            PhysicalDeviceCommObject.from(
              name = "Measurement value offset - CO2 Offset",
              datatype = DPT9,
              ioType = InOut,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-0 - General"
            ),
            PhysicalDeviceCommObject.from(
              name = "Receive measurement value - CO2 reference",
              datatype = DPT9,
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
              .from(name = "Switching - CO2 threshold 1", datatype = DPT5, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-2 - CO2 sensor"),
            PhysicalDeviceCommObject
              .from(name = "Switching - CO2 threshold 2", datatype = DPT5, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-2 - CO2 sensor"),
            PhysicalDeviceCommObject
              .from(name = "Switching - CO2 threshold 3", datatype = DPT5, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-2 - CO2 sensor"),
            PhysicalDeviceCommObject.from(
              name = "Actuating value 0-100% - Ventilation of CO2",
              datatype = DPT5,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-2 - CO2 sensor"
            ),
            PhysicalDeviceCommObject
              .from(name = "Send - CO2 scenes", datatype = DPT17, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-2 - CO2 sensor")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-3 - Humidity sensor",
          comObjects = List(
            PhysicalDeviceCommObject.from(
              name = "Switching - Humidity threshold 1",
              datatype = DPT5,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Switching - Humidity threshold 2",
              datatype = DPT5,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Switching - Humidity threshold 3",
              datatype = DPT5,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject.from(
              name = "Actuating value 0-100% - Ventilating humidity",
              datatype = DPT5,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-3 - Humidity sensor"
            ),
            PhysicalDeviceCommObject
              .from(name = "Send - Humidity scenes", datatype = DPT17, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-3 - Humidity sensor")
          )
        ),
        PhysicalDeviceNode(
          name = "Channel - CH-4 - RTR",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Send - Control actual value", datatype = DPT9, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject
              .from(name = "Receive - Operating mode preset", datatype = DPT20, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject
              .from(name = "Receive - Presence", datatype = DPT1, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject
              .from(name = "Closed=0, open=1 - Window status", datatype = DPT1, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject.from(
              name = "Save/call up - Operating mode as scene",
              datatype = DPT18,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-4 - RTR"
            ),
            PhysicalDeviceCommObject
              .from(name = "Send - Current operating mode", datatype = DPT20, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject
              .from(name = "Send - Heating actuating value", datatype = DPT5, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR"),
            PhysicalDeviceCommObject.from(
              name = "Defining the set temperature - Base setpoint",
              datatype = DPT9,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-4 - RTR"
            ),
            PhysicalDeviceCommObject.from(
              name = "Send - Setpoint offset at rotary control",
              datatype = DPT9,
              ioType = Unknown,
              physicalAddress = ("1", "1", "10"),
              deviceNodeName = "Channel - CH-4 - RTR"
            ),
            PhysicalDeviceCommObject
              .from(name = "Setting/sending - Current setpoint", datatype = DPT9, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-4 - RTR")
          )
        ),
        PhysicalDeviceNode(name = "Channel - CH-6 - External inputs", comObjects = List()),
        PhysicalDeviceNode(
          name = "Channel - CH-7 - Comparator",
          comObjects = List(
            PhysicalDeviceCommObject
              .from(name = "Output - Comparator", datatype = DPT5, ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-7 - Comparator")
          )
        ),
        PhysicalDeviceNode(
          name = "Default",
          comObjects = List(
            PhysicalDeviceCommObject.from(name = "Info - Alarm", datatype = DPTUnknown, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Default"),
            PhysicalDeviceCommObject.from(name = "Error text - Alarm", datatype = DPT16, ioType = Unknown, physicalAddress = ("1", "1", "10"), deviceNodeName = "Default")
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
              datatype = DPT1,
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
