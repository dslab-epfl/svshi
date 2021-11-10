package ch.epfl.core.compiler.parsers

import ch.epfl.core.compiler.models._
import ch.epfl.core.compiler.parsers.ets.EtsParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

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
            PhysicalDeviceChannel(
              "Obj_Switching - Channel A: Switch",
              DPT1,
              In
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Switch actuator B",
          List(
            PhysicalDeviceChannel(
              "Obj_Switching - Channel B: Switch",
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
            List(PhysicalDeviceChannel("Object 1 - Object 1", DPT5, Unknown))
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
            PhysicalDeviceChannel(
              "Obj_StellgroesseHeizen_Switch - RTC: Stellgröße Heizen",
              DPT5,
              In
            ),
            PhysicalDeviceChannel(
              "Obj_Betriebsart_Single - RTC: Betriebsmodus Normal",
              DPT20,
              In
            ),
            PhysicalDeviceChannel(
              "Obj_BetriebsartUeberlagert_Single - RTC: Betriebsmodus Übersteuerung",
              DPT20,
              In
            ),
            PhysicalDeviceChannel(
              "Obj_AktuelleIstTemperatur - RTC: Ist-Temperatur",
              DPT9,
              Out
            ),
            PhysicalDeviceChannel(
              "Obj_AktuellerSollwert - RTC: Aktueller Sollwert",
              DPT9,
              Out
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-4 - Function block 1",
          List(
            PhysicalDeviceChannel("Obj_Wert1Bit - S1: Schalten", DPT14, In)
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-5 - Function block 2",
          List(
            PhysicalDeviceChannel("Obj_Wert1Bit - S3: Schalten", DPT14, In)
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Function block 3",
          List(
            PhysicalDeviceChannel("Obj_Wert1Bit - S5: Schalten", DPT14, In)
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
            PhysicalDeviceChannel(" - Device clock", DPT1, In),
            PhysicalDeviceChannel(" - Device clock", DPT11, In),
            PhysicalDeviceChannel(" - Device clock", DPT10, In),
            PhysicalDeviceChannel(" - Device clock", DPT19, In)
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
            PhysicalDeviceChannel("Vanne - Vanne", UnknownDPT, In),
            PhysicalDeviceChannel("Vanne - Vanne", UnknownDPT, In),
            PhysicalDeviceChannel("Vanne - Vanne", UnknownDPT, In),
            PhysicalDeviceChannel(
              "Indication d'état - Indication d'état",
              UnknownDPT,
              In
            ),
            PhysicalDeviceChannel(
              "Indication d'état - Indication d'état",
              UnknownDPT,
              In
            ),
            PhysicalDeviceChannel(
              "Indication d'état - Indication d'état",
              UnknownDPT,
              In
            )
          )
        )
      )
    )

    structure.deviceInstances.contains(device1) shouldEqual true
    structure.deviceInstances.contains(device2) shouldEqual true
    structure.deviceInstances.contains(device3) shouldEqual true
    structure.deviceInstances.contains(device4) shouldEqual true
    structure.deviceInstances.contains(device5) shouldEqual true
  }
}
