package ch.epfl.core.models.physical

case class GroupAddress(mainGroup: Int, middleGroup: Int, groupAddressN: Int)

object GroupAddresses {
  private var nextMainGroup = 0 // 0 -> 31
  private var nextMiddleGroup = 0 // 0 -> 127
  private var nextGroupAddressN = 0 // 1 -> 127

  // We know from the KNX documentation that group addresses are encoded on 16 bits so let's use
  // 4 bits for the main group, 6 for the middle group and the 6 for the group address number
  // so
  private val maxMainGroup = 31
  private val maxMiddleGroup = 127
  private val maxGroupAddressN = 127

  def resetNextGroupAddress(): Unit = {
    nextMainGroup = 0
    nextMiddleGroup = 0
    nextGroupAddressN = 0
  }
  def getNextGroupAddress: GroupAddress = {
    if (nextGroupAddressN + 1 > maxGroupAddressN) {
      if (nextMiddleGroup + 1 > maxMiddleGroup) {
        if (nextMainGroup + 1 > maxMainGroup) {
          throw new NoMoreAvailableGroupAddressException
        } else {
          nextMainGroup += 1
          nextMiddleGroup = 0
        }
      } else {
        nextMiddleGroup += 1
      }
      nextGroupAddressN = 0
    }

    nextGroupAddressN += 1

    GroupAddress(nextMainGroup, nextMiddleGroup, nextGroupAddressN)
  }
}

class NoMoreAvailableGroupAddressException() extends Exception
