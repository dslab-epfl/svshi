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
              "Ausgang - Obj_StellgroesseHeizen_Switch - RTC: Stellgröße Heizen",
              DPT5,
              Out
            ),
            PhysicalDeviceCommObject.from(
              "Ein-/Ausgang - Obj_Betriebsart_Single - RTC: Betriebsmodus Normal",
              DPT20,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Eingang - Obj_BetriebsartUeberlagert_Single - RTC: Betriebsmodus Übersteuerung",
              DPT20,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Ausgang - Obj_AktuelleIstTemperatur - RTC: Ist-Temperatur",
              DPT9,
              Out
          ),
            PhysicalDeviceCommObject.from(
              "Ausgang - Obj_AktuellerSollwert - RTC: Aktueller Sollwert",
              DPT9,
              Out
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-4 - Function block 1",
          List(
            PhysicalDeviceCommObject.from(
              "Ein-/Ausgang - Obj_Wert1Bit - S1: Schalten",
              DPT14,
              InOut
            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-5 - Function block 2",
          List(
            PhysicalDeviceCommObject.from(
              "Ein-/Ausgang - Obj_Wert1Bit - S3: Schalten",
              DPT14,
              InOut

            )
          )
        ),
        PhysicalDeviceNode(
          "Channel - CH-6 - Function block 3",
          List(
            PhysicalDeviceCommObject.from(
              "Ein-/Ausgang - Obj_Wert1Bit - S5: Schalten",
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
              "Commande % - Vanne - Vanne",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Forçage % - Vanne - Vanne",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from("Arrêt - Vanne - Vanne", UnknownDPT, InOut),
            PhysicalDeviceCommObject.from(
              "Position vanne % - Indication d'état - Indication d'état",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Valeur commande la plus élevée - Indication d'état - Indication d'état",
              UnknownDPT,
              InOut
            ),
            PhysicalDeviceCommObject.from(
              "Présence / Absence commande - Indication d'état - Indication d'état",
              UnknownDPT,
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