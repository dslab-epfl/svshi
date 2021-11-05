package ch.epfl.core.compiler

import ch.epfl.core.compiler.jsonModels._
import ch.epfl.core.compiler.models._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonParserTest extends AnyFlatSpec with Matchers {
  "parseJson" should "return the correct ParsedStructure with correct input" in {
    JsonParser.parseJson(
      """
        |{
        |    "deviceTypes":
        |    [
        |        {
        |            "type" : "light",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "switch",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "button",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "on/off",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "out"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "hvac",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "percent_hot",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "percent_cool",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "heat_pump_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "cool_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name": "air_temperature",
        |                    "datatype": "DPT-9.001",
        |                    "type": "in/out"
        |                }
        |
        |            ]
        |        }
        |
        |    ],
        |    "deviceInstances":
        |    [
        |        {
        |            "type": "light",
        |            "name": "light_livingroom_1"
        |        },
        |        {
        |            "type": "light",
        |            "name": "light_bathroom_2"
        |        },
        |        {
        |            "type": "hvac",
        |            "name": "home_hvac"
        |        }
        |    ]
        |}""".stripMargin) shouldEqual
      ParsedStructureJsonParsed(
      List(
        DeviceTypeJsonParsed("light",
          List(
            ChannelJsonParsed("switch", "DPT-1.001", "in")
          )
        ),
        DeviceTypeJsonParsed("button",
          List(
            ChannelJsonParsed("on/off", "DPT-1.001", "out")
          )
        ),
        DeviceTypeJsonParsed("hvac",
          List(
            ChannelJsonParsed("percent_hot", "DPT-5.001", "in"),
            ChannelJsonParsed("percent_cool", "DPT-5.001", "in"),
            ChannelJsonParsed("heat_pump_on", "DPT-1.001", "in"),
            ChannelJsonParsed("cool_on", "DPT-1.001", "in"),
            ChannelJsonParsed("air_temperature", "DPT-9.001", "in/out")
          )
        )
      ),
        List(
          DeviceInstanceJsonParsed("light_livingroom_1", "light"),
          DeviceInstanceJsonParsed("light_bathroom_2", "light"),
          DeviceInstanceJsonParsed("home_hvac", "hvac")
        )
    )
  }
  "parseJson" should "throw JsonParsingException with incorrect input 1" in {
    an [JsonParsingException] should be thrownBy JsonParser.parseJson(
      """
        |
        |    "deviceTypes":
        |    [
        |        {
        |            "type" : "light",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "switch",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "button",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "on/off",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "out"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "hvac",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "percent_hot",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "percent_cool",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "heat_pump_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "cool_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name": "air_temperature",
        |                    "datatype": "DPT-9.001",
        |                    "type": "in/out"
        |                }
        |
        |            ]
        |        }
        |
        |    ],
        |    "deviceInstances":
        |    [
        |        {
        |            "type": "light",
        |            "name": "light_livingroom_1"
        |        },
        |        {
        |            "type": "light",
        |            "name": "light_bathroom_2"
        |        },
        |        {
        |            "type": "hvac",
        |            "name": "home_hvac"
        |        }
        |    ]
        |}""".stripMargin)
  }
  "parseJson" should "throw JsonParsingException with incorrect input 2" in {
    an [JsonParsingException] should be thrownBy JsonParser.parseJson(
      """
        |{
        |    "deviceTypes":
        |    [
        |        {
        |            "type" : "light",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "switch",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "button",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "on/off",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "out"
        |                }
        |
        |            ]
        |        },
        |        {
        |            "type" : "hvac",
        |            "channels" :
        |            [
        |                {
        |                    "name" : "percent_hot",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "percent_cool",
        |                    "datatype" : "DPT-5.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "heat_pump_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name" : "cool_on",
        |                    "datatype" : "DPT-1.001",
        |                    "type": "in"
        |                },
        |                {
        |                    "name": "air_temperature",
        |                    "datatype": "DPT-9.001",
        |                    "type": "in/out"
        |                }
        |
        |            ]
        |        }
        |
        |    ],
        |    "deviceInstances":
        |    {
        |       "a":10
        |    }
        |
        |}""".stripMargin)
  }
  "parseJson" should "throw JsonParsingException with incorrect input 3" in {
    an [JsonParsingException] should be thrownBy JsonParser.parseJson("")
  }
  "constructPrototypicalStructure" should "return the correct structure for valid input 1" in {
    val device_type = DeviceType("device_type_1", List(
      DeviceChannel("name", DPT1, In),
      DeviceChannel("name2", DPT1, Out),
      DeviceChannel("name3", DPT1, InOut),
    ))

    JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name", "DPT-1.003", "in"),
            ChannelJsonParsed("name2", "DPT-1.002", "out"),
            ChannelJsonParsed("name3", "DPT-1.023", "in/out")
          ))
        ),
        List(
          DeviceInstanceJsonParsed("instance_1", "device_type_1")
        )
      )) shouldEqual PrototypicalStructure(
      List(
        device_type
      ),
      List(
        DeviceInstance("instance_1", device_type)
      )
    )
  }

  "constructPrototypicalStructure" should "throw SystemStructureException when type of instance is not defined above" in {

    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name", "DPT-1.003", "in"),
            ChannelJsonParsed("name2", "DPT-1.002", "out"),
            ChannelJsonParsed("name3", "DPT-1.023", "in/out")
          ))
        ),
        List(
          DeviceInstanceJsonParsed("instance_1", "device_type_unknown")
        )
      ))
  }

  "constructPrototypicalStructure" should "throw SystemStructureException when a channel in a type have an i/o type different from in, out, in/out 1" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name", "DPT-1.003", "In")
          ))
        ),
        List()
      ))
  }

  "constructPrototypicalStructure" should "throw SystemStructureException when a channel in a type have an i/o type different from in, out, in/out 2" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name2", "DPT-1.002", "outt")
          ))
        ),
        List()
      ))
  }
  "constructPrototypicalStructure" should "throw SystemStructureException when a channel in a type have an i/o type different from in, out, in/out 3" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name2", "DPT-1.002", "")
          ))
        ),
        List()
      ))
  }

  "constructPrototypicalStructure" should "throw SystemStructureException when a channel in a type have an i/o type different from in, out, in/out 4" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name3", "DPT-1.023", "inout")
          ))
        ),
        List()
      ))
  }
  "constructPrototypicalStructure" should "throw SystemStructureException when a channel have an unsupported DPT 1" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name3", "DPT-42.023", "in")
          ))
        ),
        List()
      ))
  }
  "constructPrototypicalStructure" should "throw SystemStructureException when a channel have an unsupported DPT 2" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name3", "DPT321", "in")
          ))
        ),
        List()
      ))
  }
  "constructPrototypicalStructure" should "throw SystemStructureException when a channel have an unsupported DPT 3" in {
    an [SystemStructureException] should be thrownBy JsonParser.constructPrototypicalStructure(
      ParsedStructureJsonParsed(
        List(
          DeviceTypeJsonParsed("device_type_1", List(
            ChannelJsonParsed("name3", "321", "in")
          ))
        ),
        List()
      ))
  }
}
