package ch.epfl.core.compiler.parsers

import ch.epfl.core.compiler.parsers.ets.EtsParser
import ch.epfl.core.models.physical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.hashing.MurmurHash3

class EtsParserTest extends AnyFlatSpec with Matchers {
  val testFilePathString = "res/ets_project_test.knxproj"
  "parseEtsProjectFile" should "return the correct structure on the test file" in {
    val structure = EtsParser.parseEtsProjectFile(testFilePathString)
    println(structure)
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
              In
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Switch actuator B",
          List(
            PhysicalDeviceCommObject.from(
              "Switch - Obj_Switching - Channel B: Switch",
              DPT1,
              In
            )
          )
        )
      )
    )

    val device2 =
      PhysicalDevice(
        name = "STANDARDdue Taster 1-4fach Feuchtigkeitsschutz",
        address = ("1", "1", "2"),
        nodes = List(
          PhysicalDeviceNode(
            "Default",
            List(PhysicalDeviceCommObject.from("Object 1 - Object 1", DPT5, InOut))
          )
        )
      )

    val device3 = PhysicalDevice(
      name = "SBR/U6.0 HVAC-Gerät, 6fach BE",
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
              Out
            ),
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Betriebsart_Single - RTC: Normal operating mode",
              DPT20,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Input - Obj_BetriebsartUeberlagert_Single - RTC: Override operating mode",
              DPT20,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuelleIstTemperatur - RTC: Actual temperature",
              DPT9,
              Out
          ),
            PhysicalDeviceCommObject.from(
              "Output - Obj_AktuellerSollwert - RTC: actual setpoint",
              DPT9,
              Out
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-4 - Function block 1",
          List(
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Wert1Bit - S1: Switching",
              DPT14,
              InOut
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-5 - Function block 2",
          List(
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Wert1Bit - S3: Switching",
              DPT14,
              InOut

            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Function block 3",
          List(
            PhysicalDeviceCommObject.from(
              "Input/Output - Obj_Wert1Bit - S5: Switching",
              DPT14,
              InOut
            )
          )
        ),
        PhysicalDeviceNode("Channel - CH-7 - Common functions", List())
      )
    )

    val device4 = PhysicalDevice(
      name = "AC/S1.2.1 Application Controller, BACnet",
      address = ("1", "1", "4"),
      nodes = List(
        PhysicalDeviceNode(
          "Channel - CH-1 - Device clock",
          List(
            PhysicalDeviceCommObject.from("Request time - Device clock", DPT1, Unknown),
            PhysicalDeviceCommObject.from("Date - Device clock", DPT11, Out),
            PhysicalDeviceCommObject.from("Time - Device clock", DPT10, Out),
            PhysicalDeviceCommObject.from("Date/Time - Device clock", DPT19, Out)
          )
        )
      )
    )

    val device5 = PhysicalDevice(
      name = "Vanne motorisée",
      address = ("1", "2", "1"),
      nodes = List(
        PhysicalDeviceNode(
          "Default",
          List(
            PhysicalDeviceCommObject.from(
              "Command % - Vanne - Valve",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Priority % - Vanne - Valve",
              DPT1,
              InOut
            ),
            PhysicalDeviceCommObject.from("Stop - Vanne - Valve", DPT1, InOut),
            PhysicalDeviceCommObject.from(
              "Valve position % - Indication d'état - Status indication",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Highest command value - Indication d'état - Status indication",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Presence / Absence command - Indication d'état - Status indication",
              DPT1,
              InOut
            )
          )
        )
      )
    )

    structure.deviceInstances.exists(p =>
      p.name == device1.name
    ) shouldEqual true
    structure.deviceInstances.find(p =>
      p.name == device1.name
    ) shouldEqual Some(device1)
    structure.deviceInstances.exists(p =>
      p.name == device2.name
    ) shouldEqual true
    structure.deviceInstances.find(p =>
      p.name == device2.name
    ) shouldEqual Some(device2)
    structure.deviceInstances.exists(p =>
      p.name == device3.name
    ) shouldEqual true
    structure.deviceInstances.find(p =>
      p.name == device3.name
    ) shouldEqual Some(device3)
    structure.deviceInstances.exists(p =>
      p.name == device4.name
    ) shouldEqual true
    structure.deviceInstances.find(p =>
      p.name == device4.name
    ) shouldEqual Some(device4)
    structure.deviceInstances.exists(p =>
      p.name == device5.name
    ) shouldEqual true
    structure.deviceInstances.find(p =>
      p.name == device5.name
    ) shouldEqual Some(device5)
  }
}
