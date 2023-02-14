package chaincash.contracts

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.PlasmaMap
import org.ergoplatform.appkit.{ErgoId, ErgoValue}
import sigmastate.{AvlTreeFlags, Values}
import special.sigma.AvlTree

object Constants {
  private val plasmaMap = new PlasmaMap[ErgoId, Values.ErgoTree](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
  val emptyTreeErgoValue: ErgoValue[AvlTree] = plasmaMap.ergoValue
  val emptyTree: AvlTree = emptyTreeErgoValue.getValue
}
