package ch.epfl.core.parser

import ch.epfl.core.model.physical._
import ch.epfl.core.parser.ets.EtsParser
import ch.epfl.core.utils.Constants
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EtsParserTest extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  override def beforeEach(): Unit = {
    Constants.setSvshiHome(os.Path("../..", os.pwd).toString)
  }
  val testFilePathString1 = "svshi/core/res/ets_project_test.knxproj"
  val testFilePath1: os.Path = os.Path(testFilePathString1, os.pwd / os.up / os.up)
  val testFilePathString2 = "svshi/core/res/DSLAB_proto.knxproj"
  val testFilePath2: os.Path = os.Path(testFilePathString2, os.pwd / os.up / os.up)

  "parseEtsProjectFile" should "return the correct structure on the test file 1" in {
    val structure = EtsParser.parseEtsProjectFile(testFilePath1)
    val device1 = PhysicalDevice(
      name = "Switch Standard 2-fold 16A/1.0a",
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
              ("1", "1", "1")
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
              ("1", "1", "1")
            )
          )
        )
      )
    )

    val device2 =
      PhysicalDevice(
        name = "Push-button 1- to 4-gang moisture protection",
        address = ("1", "1", "2"),
        nodes = List(
          PhysicalDeviceNode(
            "Default",
            List(PhysicalDeviceCommObject.from("ON/OFF, switching - Object 1 - Object 1 - Push-button", DPT5, InOut, ("1", "1", "2")))
          )
        )
      )

    val device3 = PhysicalDevice(
      name = "HVAC device, 6gang BE/1",
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
              ("1", "1", "3")
            ),
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Betriebsart_Single - RTC: Normal operating mode",
              DPT20,
              InOut,
              ("1", "1", "3")
            ),
            PhysicalDeviceCommObject.from(
              "Input - Obj_BetriebsartUeberlagert_Single - RTC: Override operating mode",
              DPT20,
              InOut,
              ("1", "1", "3")
            ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuelleIstTemperatur - RTC: Actual temperature",
              DPT9,
              Out,
              ("1", "1", "3")
            ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuellerSollwert - RTC: actual setpoint",
              DPT9,
              Out,
              ("1", "1", "3")
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
              ("1", "1", "3")
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
              ("1", "1", "3")
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
              ("1", "1", "3")
            )
          )
        ),
        PhysicalDeviceNode("Channel - CH-7 - Common functions", List())
      )
    )

    val device4 = PhysicalDevice(
      name = "HVAC Application/1.0a with BACnet",
      address = ("1", "1", "4"),
      nodes = List(
        PhysicalDeviceNode(
          "Channel - CH-1 - Device clock",
          List(
            PhysicalDeviceCommObject.from("Request time - Device clock", DPT1, Unknown, ("1", "1", "4")),
            PhysicalDeviceCommObject.from("Date - Device clock", DPT11, Out, ("1", "1", "4")),
            PhysicalDeviceCommObject.from("Time - Device clock", DPT10, Out, ("1", "1", "4")),
            PhysicalDeviceCommObject.from("Date/Time - Device clock", DPT19, Out, ("1", "1", "4"))
          )
        )
      )
    )

    val device5 = PhysicalDevice(
      name = "TL501A V1.1",
      address = ("1", "2", "1"),
      nodes = List(
        PhysicalDeviceNode(
          "Default",
          List(
            PhysicalDeviceCommObject.from(
              "Command % - Vanne - Valve",
              DPTUnknown,
              InOut,
              ("1", "2", "1")
            ),
            PhysicalDeviceCommObject.from(
              "Priority % - Vanne - Valve",
              DPT1,
              InOut,
              ("1", "2", "1")
            ),
            PhysicalDeviceCommObject.from("Stop - Vanne - Valve", DPT1, InOut, ("1", "2", "1")),
            PhysicalDeviceCommObject.from(
              "Valve position % - Indication d'état - Status indication",
              DPTUnknown,
              InOut,
              ("1", "2", "1")
            ),
            PhysicalDeviceCommObject.from(
              "Highest command value - Indication d'état - Status indication",
              DPTUnknown,
              InOut,
              ("1", "2", "1")
            ),
            PhysicalDeviceCommObject.from(
              "Presence / Absence command - Indication d'état - Status indication",
              DPT1,
              InOut,
              ("1", "2", "1")
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

    val device1 = PhysicalDevice("IP Interface/2.1a",("1","1","1"),List(
      PhysicalDeviceNode("Channel - CH-0 - IP settings",List())))

    val device2 = PhysicalDevice("Binary Input Display Heat 2f/1.3c",("1","1","7"),List(
      PhysicalDeviceNode("Default",List(
        PhysicalDeviceCommObject.from("Disable - Eingang A - Input A",DPT1,In, ("1","1","7")),
        PhysicalDeviceCommObject.from("Telegr. counter value 2 bytes - Telegr. switch - Eingang A - Input A",DPTUnknown,InOut, ("1","1","7")),
        PhysicalDeviceCommObject.from("Disable - Eingang B - Input B",DPT1,In, ("1","1","7")),
        PhysicalDeviceCommObject.from("Control value (PWM) - Telegr. switch - Ausgang B - Output B - Input B",DPTUnknown,In, ("1","1","7"))))))

    val device3 = PhysicalDevice("LUXORliving S1",("1","1","8"),List(
      PhysicalDeviceNode("Channel - CH-1 - Channel C1",List(
        PhysicalDeviceCommObject.from("Threshold as a percentage - Switch object - Channel C1",DPT1,In,("1","1","8")),
        PhysicalDeviceCommObject.from("Feedback On/Off - Channel C1",DPT1,Out,("1","1","8")))),
      PhysicalDeviceNode("Channel - CH-2 - Input I1",List(
        PhysicalDeviceCommObject.from("Switching - Channel I1",DPT1,Out,("1","1","8")))),
      PhysicalDeviceNode("Channel - CH-3 - Input I2",List(
        PhysicalDeviceCommObject.from("Switching - Channel I2",DPT1,Out,("1","1","8")))),
      PhysicalDeviceNode("Default",List(
        PhysicalDeviceCommObject.from("Excess temperature - Alarm",DPT1,Out,("1","1","8")),
        PhysicalDeviceCommObject.from("ON - Central permanent",DPT1,In,("1","1","8")),
        PhysicalDeviceCommObject.from("OFF - Central switching",DPT1,In,("1","1","8"))))))

    val device4 = PhysicalDevice("AMUN 716 S",("1","1","10"),List(
      PhysicalDeviceNode("Channel - CH-0 - General",List(
        PhysicalDeviceCommObject.from("Version - Firmware",DPTUnknown,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - CO2 value",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Relative humidity",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Air pressure",DPT14,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Degree of comfort",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Temperature value",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Measurement value offset - CO2 Offset",DPT9,InOut,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Receive measurement value - CO2 reference",DPT9,InOut,("1", "1", "10")))),
      PhysicalDeviceNode("Channel - CH-2 - CO2 sensor",List(
        PhysicalDeviceCommObject.from("Switching - CO2 threshold 1",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Switching - CO2 threshold 2",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Switching - CO2 threshold 3",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Actuating value 0-100% - Ventilation of CO2",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - CO2 scenes",DPT17,Unknown,("1", "1", "10")))),
      PhysicalDeviceNode("Channel - CH-3 - Humidity sensor",List(
        PhysicalDeviceCommObject.from("Switching - Humidity threshold 1",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Switching - Humidity threshold 2",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Switching - Humidity threshold 3",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Actuating value 0-100% - Ventilating humidity",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Humidity scenes",DPT17,Unknown,("1", "1", "10")))),
      PhysicalDeviceNode("Channel - CH-4 - RTR",List(
        PhysicalDeviceCommObject.from("Send - Control actual value",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Receive - Operating mode preset",DPT20,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Receive - Presence",DPT1,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Closed=0, open=1 - Window status",DPT1,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Save/call up - Operating mode as scene",DPT18,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Current operating mode",DPT20,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Heating actuating value",DPT5,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Defining the set temperature - Base setpoint",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Send - Setpoint offset at rotary control",DPT9,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Setting/sending - Current setpoint",DPT9,Unknown,("1", "1", "10")))),
      PhysicalDeviceNode("Channel - CH-6 - External inputs",List()),
      PhysicalDeviceNode("Channel - CH-7 - Comparator",List(
        PhysicalDeviceCommObject.from("Output - Comparator",DPT5,Out,("1", "1", "10")))),
      PhysicalDeviceNode("Default",List(
        PhysicalDeviceCommObject.from("Info - Alarm",DPTUnknown,Unknown,("1", "1", "10")),
        PhysicalDeviceCommObject.from("Error text - Alarm",DPT16,Unknown,("1", "1", "10"))))))

    structure.deviceInstances.exists(p => p.address == device1.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device1.address) shouldEqual Some(device1)
    structure.deviceInstances.exists(p => p.address == device2.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device2.address) shouldEqual Some(device2)
    structure.deviceInstances.exists(p => p.address == device3.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device3.address) shouldEqual Some(device3)
    structure.deviceInstances.exists(p => p.address == device4.address) shouldEqual true
    structure.deviceInstances.find(p => p.address == device4.address) shouldEqual Some(device4)
  }
}
