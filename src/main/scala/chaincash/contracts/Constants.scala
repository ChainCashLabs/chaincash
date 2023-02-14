package chaincash.contracts

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.PlasmaMap
import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.script.ScriptUtil
import org.ergoplatform.appkit.{ErgoId, ErgoValue}
import sigmastate.{AvlTreeFlags, Values}
import special.sigma.AvlTree

object Constants {
  private val plasmaMap = new PlasmaMap[ErgoId, Values.ErgoTree](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
  val emptyTreeErgoValue: ErgoValue[AvlTree] = plasmaMap.ergoValue
  val emptyTree: AvlTree = emptyTreeErgoValue.getValue

  val noteContract = scala.io.Source.fromFile("contracts/note.es", "utf-8").getLines.mkString("\n")

  val reserveContract = scala.io.Source.fromFile("contracts/reserve.es", "utf-8").getLines.mkString("\n")

  val noteErgoTree = ScriptUtil.compile(Map.empty, noteContract)
  val noteAddress = getStringFromAddress(getAddressFromErgoTree(noteErgoTree))

  val reserveErgoTree = ScriptUtil.compile(Map.empty, reserveContract)
  val reserveAddress = getStringFromAddress(getAddressFromErgoTree(reserveErgoTree))
}
