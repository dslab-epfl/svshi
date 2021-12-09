package ch.epfl.core.parser.ets

case class ParsedDevice(address: (String, String, String), name: String, io: List[ChannelNode])

case class ChannelNode(name: String, ioPorts: List[IOPort])
case class IOPort(name: String, dpt: String, inOutType: String, objectSizeString: String)