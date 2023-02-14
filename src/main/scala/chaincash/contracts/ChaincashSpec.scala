package chaincash.contracts

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil

object ChaincashSpec extends App {

  val noteContract = scala.io.Source.fromFile("contracts/note.es", "utf-8").getLines.mkString("\n")

  val reserveContract = scala.io.Source.fromFile("contracts/reserve.es", "utf-8").getLines.mkString("\n")

  val noteErgoTree = ScriptUtil.compile(Map.empty, noteContract)
  val noteAddress = getStringFromAddress(getAddressFromErgoTree(noteErgoTree))
  println("Note contract address: " + noteAddress)

  val reserveErgoTree = ScriptUtil.compile(Map.empty, reserveContract)
  val reserveAddress = getStringFromAddress(getAddressFromErgoTree(reserveErgoTree))
  println("Reserve contract address: " + reserveAddress)

}
