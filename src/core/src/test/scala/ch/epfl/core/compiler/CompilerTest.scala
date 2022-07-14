package ch.epfl.core.compiler

import ch.epfl.core.model.bindings.GroupAddressAssignment
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.model.python.{PythonBool, PythonFloat}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Using

class CompilerTest extends AnyFlatSpec with Matchers {
  "generateGroupAddressesList" should "write the correct json file" in {
    val physStruct = PhysicalStructure(Nil)
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321)),
            DeviceInstanceBinding("device5", BinarySensorBinding(BinarySensor.toString, 323))
          )
        )
      )
    )
    val physIdToGA = List(
      (311, GroupAddress(3, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (322, GroupAddress(3, 2, 2)),
      (321, GroupAddress(3, 2, 1)),
      (323, GroupAddress(3, 2, 3))
    ).toMap

    val assignment = GroupAddressAssignment(physStruct, appLibraryBindings, physIdToGA)
    val outputPath = os.Path("res/test_group_address_list_write.json", os.pwd)
    Compiler.generateGroupAddressesList(assignment, outputPath)
    val parsed = Using(Source.fromFile(outputPath.toIO)) { fileBuff =>
      try {
        upickle.default.read[GroupAddressesList](fileBuff.getLines().mkString)
      } catch {
        case e: Exception =>
          fail(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\n$e")
      }
    }.get
    parsed.addresses.length shouldEqual 5

    parsed.addresses.map(_._1).contains(GroupAddress(3, 1, 1).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(2, 1, 2).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(3, 2, 2).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(3, 2, 1).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(3, 2, 3).toString) shouldBe true

    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 1, 1).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(2, 1, 2).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 2, 2).toString) shouldEqual (PythonFloat.toString, DPT9(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 2, 1).toString) shouldEqual (PythonFloat.toString, DPT9(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 2, 3).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
  }

  "generateGroupAddressesList" should "write the correct json file when multiple conflicting types" in {
    val physStruct = PhysicalStructure(Nil)
    val appLibraryBindings = AppLibraryBindings(
      List(
        AppPrototypeBindings(
          "app1",
          List(
            DeviceInstanceBinding("device1", BinarySensorBinding(BinarySensor.toString, 311)),
            DeviceInstanceBinding("device2", SwitchBinding(Switch.toString, 212)),
            DeviceInstanceBinding("device3", TemperatureSensorBinding(TemperatureSensor.toString, 322)),
            DeviceInstanceBinding("device4", HumiditySensorBinding(HumiditySensor.toString, 321)),
            DeviceInstanceBinding("device5", BinarySensorBinding(BinarySensor.toString, 321))
          )
        )
      )
    )
    val physIdToGA = List(
      (311, GroupAddress(3, 1, 1)),
      (212, GroupAddress(2, 1, 2)),
      (322, GroupAddress(3, 2, 2)),
      (321, GroupAddress(3, 2, 1))
    ).toMap

    val assignment = GroupAddressAssignment(physStruct, appLibraryBindings, physIdToGA)
    val outputPath = os.Path("res/test_group_address_list_write.json", os.pwd)
    Compiler.generateGroupAddressesList(assignment, outputPath)
    val parsed = Using(Source.fromFile(outputPath.toIO)) { fileBuff =>
      try {
        upickle.default.read[GroupAddressesList](fileBuff.getLines().mkString)
      } catch {
        case e: Exception =>
          fail(s"The given Json is not parsable, it has either a syntax error or the wrong structure.\n$e")
      }
    }.get
    parsed.addresses.length shouldEqual 4

    parsed.addresses.map(_._1).contains(GroupAddress(3, 1, 1).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(2, 1, 2).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(3, 2, 2).toString) shouldBe true
    parsed.addresses.map(_._1).contains(GroupAddress(3, 2, 1).toString) shouldBe true

    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 1, 1).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(2, 1, 2).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 2, 2).toString) shouldEqual (PythonFloat.toString, DPT9(-1).toString)
    parsed.addresses.map(t => (t._1, (t._2, t._3))).toMap.apply(GroupAddress(3, 2, 1).toString) shouldEqual (PythonBool.toString, DPT1(-1).toString)
  }
}
