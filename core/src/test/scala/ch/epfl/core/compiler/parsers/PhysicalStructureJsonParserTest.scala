package ch.epfl.core.compiler.parsers

import ch.epfl.core.compiler.parsers.json.physical.PhysicalStructureJsonParser.{constructPhysicalStructure, parseJson}
import ch.epfl.core.compiler.parsers.json.physical._
import ch.epfl.core.models.physical._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Using

class PhysicalStructureJsonParserTest extends AnyFlatSpec with Matchers {
  "parseJson" should "return the correct PhysicalStructureJson with correct input" in {
    val json =
      """
        |{
        |    "deviceInstances":
        |    [
        |       {
        |         "name": "device1",
        |         "address": "1.1.1",
        |         "nodes":
        |           [
        |             {
        |               "name" : "device1Node1",
        |               "comObjects":
        |                 [
        |                   {
        |                     "name": "device1Node1ComObj1",
        |                     "datatype": "DPT-1",
        |                     "ioType" : "in"
        |                   }
        |                 ]
        |             }
        |           ]
        |       },
        |       {
        |         "name": "device2",
        |         "address": "1.1.2",
        |         "nodes":
        |           [
        |             {
        |               "name" : "device2Node1",
        |               "comObjects":
        |                 [
        |                   {
        |                     "name": "device2Node1ComObj1",
        |                     "datatype": "DPT-2",
        |                     "ioType" : "out"
        |                   },
        |                   {
        |                     "name": "device2Node1ComObj2",
        |                     "datatype": "DPT-5",
        |                     "ioType" : "in"
        |                   }
        |                 ]
        |             }
        |           ]
        |       }
        |    ]
        |}
        |
        |""".stripMargin

    val device1 = PhysicalDeviceJson(
      "device1",
      "1.1.1",
      List(
        PhysicalDeviceNodeJson(
          "device1Node1",
          List(
            PhysicalDeviceCommObjectJson("device1Node1ComObj1", "DPT-1", "in")
          )
        )
      )
    )
    val device2 = PhysicalDeviceJson(
      "device2",
      "1.1.2",
      List(
        PhysicalDeviceNodeJson(
          "device2Node1",
          List(
            PhysicalDeviceCommObjectJson("device2Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device2Node1ComObj2", "DPT-5", "in")
          )
        )
      )
    )
    val app = PhysicalStructureJson(List(device1, device2))
    PhysicalStructureJsonParser.parseJson(json) shouldEqual app
  }

  "constructPhysicalStructure" should "return the correct PhysicalStructure with correct input" in {
    val device1 = PhysicalDeviceJson(
      "device1",
      "1.1.1",
      List(
        PhysicalDeviceNodeJson(
          "device1Node1",
          List(
            PhysicalDeviceCommObjectJson("device1Node1ComObj1", "DPT-1", "in")
          )
        )
      )
    )
    val device2 = PhysicalDeviceJson(
      "device2",
      "1.1.2",
      List(
        PhysicalDeviceNodeJson(
          "device2Node1",
          List(
            PhysicalDeviceCommObjectJson("device2Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device2Node1ComObj2", "DPT-5", "in")
          )
        )
      )
    )
    val device3 = PhysicalDeviceJson(
      "device3",
      "1.1.3",
      List(
        PhysicalDeviceNodeJson(
          "device3Node1",
          List(
            PhysicalDeviceCommObjectJson("device3Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device3Node1ComObj2", "DPT-5", "in")
          )
        ),
        PhysicalDeviceNodeJson(
          "device3Node2",
          List(
            PhysicalDeviceCommObjectJson("device3Node2ComObj1", "DPT-5", "in"),
            PhysicalDeviceCommObjectJson(
              "device3Node2ComObj2",
              "DPT-12",
              "in/out"
            )
          )
        )
      )
    )
    val app = PhysicalStructureJson(List(device1, device2, device3))

    val device1After = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In))
        )
      )
    )
    val device2After = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In)
          )
        )
      )
    )
    val device3After = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut)
          )
        )
      )
    )
    val physicalStructure =
      PhysicalStructure(List(device1After, device2After, device3After))

    PhysicalStructureJsonParser.constructPhysicalStructure(
      app
    ) shouldEqual physicalStructure
  }

  "physicalStructureToJson" should "return the correct json structure for correct input" in {
    val device1 = PhysicalDeviceJson(
      "device1",
      "1.1.1",
      List(
        PhysicalDeviceNodeJson(
          "device1Node1",
          List(
            PhysicalDeviceCommObjectJson("device1Node1ComObj1", "DPT-1", "in")
          )
        )
      )
    )
    val device2 = PhysicalDeviceJson(
      "device2",
      "1.1.2",
      List(
        PhysicalDeviceNodeJson(
          "device2Node1",
          List(
            PhysicalDeviceCommObjectJson("device2Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device2Node1ComObj2", "DPT-5", "in")
          )
        )
      )
    )
    val device3 = PhysicalDeviceJson(
      "device3",
      "1.1.3",
      List(
        PhysicalDeviceNodeJson(
          "device3Node1",
          List(
            PhysicalDeviceCommObjectJson("device3Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device3Node1ComObj2", "DPT-5", "in")
          )
        ),
        PhysicalDeviceNodeJson(
          "device3Node2",
          List(
            PhysicalDeviceCommObjectJson("device3Node2ComObj1", "DPT-5", "in"),
            PhysicalDeviceCommObjectJson(
              "device3Node2ComObj2",
              "DPT-12",
              "in/out"
            )
          )
        )
      )
    )
    val app = PhysicalStructureJson(List(device1, device2, device3))

    val device1After = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In))
        )
      )
    )
    val device2After = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In)
          )
        )
      )
    )
    val device3After = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut)
          )
        )
      )
    )
    val physicalStructure =
      PhysicalStructure(List(device1After, device2After, device3After))

    PhysicalStructureJsonParser.physicalStructureToJson(
      physicalStructure
    ) shouldEqual app
  }

  "writeToFile" should "write the correct content to the given file" in {
    val device1 = PhysicalDeviceJson(
      "device1",
      "1.1.1",
      List(
        PhysicalDeviceNodeJson(
          "device1Node1",
          List(
            PhysicalDeviceCommObjectJson("device1Node1ComObj1", "DPT-1", "in")
          )
        )
      )
    )
    val device2 = PhysicalDeviceJson(
      "device2",
      "1.1.2",
      List(
        PhysicalDeviceNodeJson(
          "device2Node1",
          List(
            PhysicalDeviceCommObjectJson("device2Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device2Node1ComObj2", "DPT-5", "in")
          )
        )
      )
    )
    val device3 = PhysicalDeviceJson(
      "device3",
      "1.1.3",
      List(
        PhysicalDeviceNodeJson(
          "device3Node1",
          List(
            PhysicalDeviceCommObjectJson("device3Node1ComObj1", "DPT-2", "out"),
            PhysicalDeviceCommObjectJson("device3Node1ComObj2", "DPT-5", "in")
          )
        ),
        PhysicalDeviceNodeJson(
          "device3Node2",
          List(
            PhysicalDeviceCommObjectJson("device3Node2ComObj1", "DPT-5", "in"),
            PhysicalDeviceCommObjectJson(
              "device3Node2ComObj2",
              "DPT-12",
              "in/out"
            )
          )
        )
      )
    )
    val app = PhysicalStructureJson(List(device1, device2, device3))

    val device1After = PhysicalDevice(
      "device1",
      ("1", "1", "1"),
      List(
        PhysicalDeviceNode(
          "device1Node1",
          List(PhysicalDeviceCommObject("device1Node1ComObj1", DPT1, In))
        )
      )
    )
    val device2After = PhysicalDevice(
      "device2",
      ("1", "1", "2"),
      List(
        PhysicalDeviceNode(
          "device2Node1",
          List(
            PhysicalDeviceCommObject("device2Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device2Node1ComObj2", DPT5, In)
          )
        )
      )
    )
    val device3After = PhysicalDevice(
      "device3",
      ("1", "1", "3"),
      List(
        PhysicalDeviceNode(
          "device3Node1",
          List(
            PhysicalDeviceCommObject("device3Node1ComObj1", DPT2, Out),
            PhysicalDeviceCommObject("device3Node1ComObj2", DPT5, In)
          )
        ),
        PhysicalDeviceNode(
          "device3Node2",
          List(
            PhysicalDeviceCommObject("device3Node2ComObj1", DPT5, In),
            PhysicalDeviceCommObject("device3Node2ComObj2", DPT12, InOut)
          )
        )
      )
    )
    val physicalStructure = PhysicalStructure(List(device1After, device2After, device3After))
    val filePath = "res/test_physicalJson.txt"
    PhysicalStructureJsonParser.writeToFile(filePath, physicalStructure)
    Using(Source.fromFile(filePath)) { fileBuff =>
      parseJson(fileBuff.mkString) shouldEqual app
    }
  }
}
