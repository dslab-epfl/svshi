package ch.epfl.core.compiler.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GroupAddressTest extends AnyFlatSpec with Matchers {
  "getNextGroupAddress" should "sends the right sequence of addresses" in {
    GroupAddresses.resetNextGroupAddress()
    for(i <- 0 to 31){
      for(j <- 0 to 127){
        for(k <- 1 to 127){
          GroupAddresses.getNextGroupAddress shouldEqual GroupAddress(i, j, k)
        }
      }
    }
  }

  "getNextGroupAddress" should "throw NoMoreAvailableGroupAddressException if called after last group address has been output" in {
    GroupAddresses.resetNextGroupAddress()
    for(_ <- 0 to 31){
      for(_ <- 0 to 127){
        for(_ <- 1 to 127){
          GroupAddresses.getNextGroupAddress
        }
      }
    }
    an [NoMoreAvailableGroupAddressException] should be thrownBy GroupAddresses.getNextGroupAddress
  }
}
