package ch.epfl.core.parser.ets

/** Classes used while parsing ETS project
  */
case class ParsedDevice(address: (String, String, String), name: String, io: List[ChannelNode])

case class ChannelNode(name: String, ioPorts: List[CommObject])
case class CommObject(name: String, dpt: String, inOutType: String, objectSizeString: String)
