package ch.epfl.core.compiler.parsers.ets

case class ParsedDevice(address: (String, String, String), name: String, io: List[ChannelNode])

case class ChannelNode(name: String, ioPorts: List[IOPort])
case class IOPort(name: String, dpt: String)