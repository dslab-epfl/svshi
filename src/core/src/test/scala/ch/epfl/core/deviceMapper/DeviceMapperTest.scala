package ch.epfl.core.deviceMapper

import ch.epfl.core.deviceMapper.model.{DeviceMapping, StructureMapping, SupportedDeviceMapping, SupportedDeviceMappingNode}
import ch.epfl.core.model.physical._
import ch.epfl.core.model.prototypical._
import ch.epfl.core.parser.json.physical.PhysicalStructureJsonParser
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DeviceMapperTest extends AnyFlatSpec with Matchers {
  val commObjectCo21 = PhysicalDeviceCommObject
    .from(name = "CO2 value - Send", datatype = DPT9(8), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General")
  val commObjectHumidity1 = PhysicalDeviceCommObject
    .from(name = "Relative humidity - Send", datatype = DPT9(7), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General")
  val commObjectDegreComfort1 = PhysicalDeviceCommObject
    .from(name = "Degree of comfort - Send", datatype = DPT5(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General")
  val commObjectTemperature1 = PhysicalDeviceCommObject
    .from(name = "Temperature value - Send", datatype = DPT9(1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General")
  val commObjectCo22 = PhysicalDeviceCommObject.from(
    name = "CO2 reference - Receive measurement value",
    datatype = DPT9(8),
    ioType = InOut,
    physicalAddress = ("1", "1", "10"),
    deviceNodeName = "Channel - CH-0 - General"
  )
  val commObjectVentilation1 = PhysicalDeviceCommObject.from(
    name = "Ventilation of CO2 - Actuating value 0-100%",
    datatype = DPT5(1),
    ioType = Out,
    physicalAddress = ("1", "1", "10"),
    deviceNodeName = "Channel - CH-2 - CO2 sensor"
  )
  val deviceKnxMulti = PhysicalDevice(
    name = "Set basic KNX Multi",
    address = ("1", "1", "10"),
    nodes = List(
      PhysicalDeviceNode(
        name = "Channel - CH-0 - General",
        comObjects = List(
          PhysicalDeviceCommObject
            .from(name = "Firmware - Version", datatype = DPTUnknown(-1), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
          commObjectCo21,
          commObjectHumidity1,
          PhysicalDeviceCommObject
            .from(name = "Air pressure - Send", datatype = DPT14(58), ioType = Out, physicalAddress = ("1", "1", "10"), deviceNodeName = "Channel - CH-0 - General"),
          commObjectDegreComfort1,
          commObjectTemperature1,
          PhysicalDeviceCommObject.from(
            name = "CO2 Offset - Measurement value offset",
            datatype = DPT9(-1),
            ioType = InOut,
            physicalAddress = ("1", "1", "10"),
            deviceNodeName = "Channel - CH-0 - General"
          ),
          commObjectCo22
        )
      ),
      PhysicalDeviceNode(
        name = "Channel - CH-2 - CO2 sensor",
        comObjects = List(
          commObjectVentilation1
        )
      )
    )
  )
  val expectedKnxMulti = DeviceMapping(
    "1.1.10",
    List(
      SupportedDeviceMappingNode(
        "Channel - CH-0 - General",
        List(
          SupportedDeviceMapping(commObjectCo21.name, CO2Sensor.toString, commObjectCo21.id),
          SupportedDeviceMapping(commObjectHumidity1.name, HumiditySensor.toString, commObjectHumidity1.id),
          SupportedDeviceMapping(commObjectDegreComfort1.name, DimmerSensor.toString, commObjectDegreComfort1.id),
          SupportedDeviceMapping(commObjectTemperature1.name, TemperatureSensor.toString, commObjectTemperature1.id),
          SupportedDeviceMapping(commObjectCo22.name, CO2Sensor.toString, commObjectCo22.id)
        )
      ),
      SupportedDeviceMappingNode(
        "Channel - CH-2 - CO2 sensor",
        List(SupportedDeviceMapping(commObjectVentilation1.name, DimmerSensor.toString, commObjectVentilation1.id))
      )
    )
  )

  val channelNameKlix1 = "Channel - MD-2_M-1_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix1 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix1
  )
  val channelNameKlix2 = "Channel - MD-2_M-2_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix2 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix2
  )
  val channelNameKlix3 = "Channel - MD-2_M-3_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix3 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix3
  )
  val channelNameKlix4 = "Channel - MD-2_M-4_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix4 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix4
  )
  val channelNameKlix5 = "Channel - MD-2_M-5_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix5 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix5
  )
  val channelNameKlix6 = "Channel - MD-2_M-6_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix6 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix6
  )
  val channelNameKlix7 = "Channel - MD-2_M-7_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix7 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff",
    datatype = DPT1(1),
    ioType = Out,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix7
  )
  val channelNameKlix8 = "Channel - MD-2_M-8_MI-1_CH-argCH - CH-{{argCH}}"
  val commObjectKlix8 = PhysicalDeviceCommObject.from(
    name = "CH-{{argCH}} - Switching : OnOff - switch and sensor",
    datatype = DPT1(1),
    ioType = InOut,
    physicalAddress = ("1", "1", "1"),
    deviceNodeName = channelNameKlix8
  )
  val deviceKlix = PhysicalDevice(
    "KliX (D4)",
    ("1", "1", "1"),
    List(
      PhysicalDeviceNode(
        channelNameKlix1,
        List(
          commObjectKlix1
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix2,
        List(
          commObjectKlix2
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix3,
        List(
          commObjectKlix3
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix4,
        List(
          commObjectKlix4
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix5,
        List(
          commObjectKlix5
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix6,
        List(
          commObjectKlix6
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix7,
        List(
          commObjectKlix7
        )
      ),
      PhysicalDeviceNode(
        channelNameKlix8,
        List(
          commObjectKlix8
        )
      )
    )
  )
  val expectedKlix = DeviceMapping(
    "1.1.1",
    List(
      SupportedDeviceMappingNode(
        name = channelNameKlix1,
        List(SupportedDeviceMapping(name = commObjectKlix1.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix1.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix2,
        List(SupportedDeviceMapping(name = commObjectKlix2.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix2.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix3,
        List(SupportedDeviceMapping(name = commObjectKlix3.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix3.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix4,
        List(SupportedDeviceMapping(name = commObjectKlix4.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix4.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix5,
        List(SupportedDeviceMapping(name = commObjectKlix5.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix5.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix6,
        List(SupportedDeviceMapping(name = commObjectKlix6.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix6.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix7,
        List(SupportedDeviceMapping(name = commObjectKlix7.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix7.id))
      ),
      SupportedDeviceMappingNode(
        name = channelNameKlix8,
        List(
          SupportedDeviceMapping(name = commObjectKlix8.name, supportedDeviceName = Switch.toString, physicalCommObjectId = commObjectKlix8.id),
          SupportedDeviceMapping(name = commObjectKlix8.name, supportedDeviceName = BinarySensor.toString, physicalCommObjectId = commObjectKlix8.id)
        )
      )
    )
  )

  "dptIoTypeToSupportedDevice" should "return switch for any dpt-1 and in" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(-1), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(12), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(32), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(42), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(2), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(3), In) should contain theSameElementsAs List(Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(4), In) should contain theSameElementsAs List(Switch)
  }

  "dptIoTypeToSupportedDevice" should "return binarySensor for any dpt-1 and out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(-1), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(12), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(32), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(42), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(2), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(3), Out) should contain theSameElementsAs List(BinarySensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(4), Out) should contain theSameElementsAs List(BinarySensor)
  }

  "dptIoTypeToSupportedDevice" should "return binarySensor and switch for any dpt-1 and in/out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(-1), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(12), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(32), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(42), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(2), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(3), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT1(4), InOut) should contain theSameElementsAs List(BinarySensor, Switch)
  }

  "dptIoTypeToSupportedDevice" should "return temperatureSensor for dpt-9-1 and dpt-9-27 and out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(1), Out) should contain theSameElementsAs List(TemperatureSensor)
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(27), Out) should contain theSameElementsAs List(TemperatureSensor)
  }

  "dptIoTypeToSupportedDevice" should "return Nil for dpt-9-42 and any type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(42), Out) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(42), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(42), InOut) shouldEqual Nil
  }

  "dptIoTypeToSupportedDevice" should "return co2Sensor for dpt-9-8 and out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(8), Out) should contain theSameElementsAs List(CO2Sensor)
  }

  "dptIoTypeToSupportedDevice" should "return humiditySensor for dpt-9-7 and out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT9(7), Out) should contain theSameElementsAs List(HumiditySensor)
  }

  "dptIoTypeToSupportedDevice" should "return dimmerSensor for dpt-5-1 and out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(1), Out) should contain theSameElementsAs List(DimmerSensor)
  }

  "dptIoTypeToSupportedDevice" should "return dimmerActuator for dpt-5-1 and in type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(1), In) should contain theSameElementsAs List(DimmerActuator)
  }

  "dptIoTypeToSupportedDevice" should "return dimmerActuator and dimmerSensor for dpt-5-1 and in/out type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(1), InOut) should contain theSameElementsAs List(DimmerSensor, DimmerActuator)
  }
  "dptIoTypeToSupportedDevice" should "return Nil for dpt-5-42 and any type" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(42), Out) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(42), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPT5(42), InOut) shouldEqual Nil
  }

  "dptIoTypeToSupportedDevice" should "return None for dpt-unknown" in {
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(-1), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(-1), InOut) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(-1), Out) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(1), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(1), InOut) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(1), Out) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(12), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(12), InOut) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(12), Out) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(42), In) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(42), InOut) shouldEqual Nil
    DeviceMapper.dptIoTypeToSupportedDevice(DPTUnknown(42), Out) shouldEqual Nil
  }
  "mapCommObject" should "return the correct mapping for a commObject of a supported type" in {
    val name = "this is a comm object name"
    val dpt = DPT5(1)
    val ioType = Out
    val id = 1234
    val commObject = PhysicalDeviceCommObject(name = name, datatype = dpt, ioType = ioType, id = id)

    DeviceMapper.mapCommObject(commObject) should contain theSameElementsAs List(SupportedDeviceMapping(name, DimmerSensor.toString, id))
  }
  "mapCommObject" should "return the Nil for a commObject of a non supported type" in {
    val name = "this is a comm object name of non supported type"
    val dpt = DPT6(1)
    val ioType = Out
    val id = 424242
    val commObject = PhysicalDeviceCommObject(name = name, datatype = dpt, ioType = ioType, id = id)

    DeviceMapper.mapCommObject(commObject) shouldEqual Nil
  }
  "mapCommObject" should "return a List with binarySensor and switch for a commObject of type DPT1 and in/out" in {
    val name = "commObject dpt1"
    val dpt = DPT1(1)
    val ioType = InOut
    val id = 424242
    val commObject = PhysicalDeviceCommObject(name = name, datatype = dpt, ioType = ioType, id = id)

    DeviceMapper.mapCommObject(commObject) shouldEqual List(SupportedDeviceMapping(name, Switch.toString, id), SupportedDeviceMapping(name, BinarySensor.toString, id))
  }
  "mapCommObject" should "return a List with binarySensor for a commObject of type DPT1 and out" in {
    val name = "commObject dpt1"
    val dpt = DPT1(1)
    val ioType = Out
    val id = 424242
    val commObject = PhysicalDeviceCommObject(name = name, datatype = dpt, ioType = ioType, id = id)

    DeviceMapper.mapCommObject(commObject) shouldEqual List(SupportedDeviceMapping(name, BinarySensor.toString, id))
  }
  "mapCommObject" should "return a List with binarySensor for a commObject of type DPT1 and in" in {
    val name = "commObject dpt1"
    val dpt = DPT1(1)
    val ioType = In
    val id = 424242
    val commObject = PhysicalDeviceCommObject(name = name, datatype = dpt, ioType = ioType, id = id)

    DeviceMapper.mapCommObject(commObject) shouldEqual List(SupportedDeviceMapping(name, Switch.toString, id))
  }

  "mapDevice" should "return the correct mapping for a device 1" in {
    DeviceMapper.mapDevice(deviceKlix) shouldEqual expectedKlix
  }

  "mapDevice" should "return the correct mapping for a device 2" in {
    DeviceMapper.mapDevice(deviceKnxMulti) shouldEqual expectedKnxMulti
  }

  "mapStructure" should "return the correct mapping for a structure 1" in {
    val structure = PhysicalStructure(List(deviceKlix, deviceKnxMulti))
    DeviceMapper.mapStructure(structure) shouldEqual StructureMapping(PhysicalStructureJsonParser.physicalStructureToJson(structure), List(expectedKlix, expectedKnxMulti))
  }

}
