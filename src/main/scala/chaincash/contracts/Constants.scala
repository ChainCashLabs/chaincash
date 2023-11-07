package chaincash.contracts

import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.PlasmaMap
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{AppkitHelpers, ErgoValue, NetworkType}
import sigmastate.eval.CGroupElement
import sigmastate.basics.CryptoConstants
import sigmastate.AvlTreeFlags
import sigmastate.Values.ErgoTree
import sigmastate.lang.{CompilerSettings, SigmaCompiler, TransformingSigmaBuilder}
import special.sigma.{AvlTree, GroupElement}

import java.util

object Constants {

  val networkType = NetworkType.MAINNET
  val networkPrefix = networkType.networkPrefix
  val ergoAddressEncoder = new ErgoAddressEncoder(networkPrefix)
  private val compiler = SigmaCompiler(CompilerSettings(networkPrefix, TransformingSigmaBuilder, lowerMethodCalls = true))

  def getAddressFromErgoTree(ergoTree: ErgoTree) = ergoAddressEncoder.fromProposition(ergoTree).get

  def compile(ergoScript: String): ErgoTree = {
    AppkitHelpers.compile(new util.HashMap[String, Object](), ergoScript, networkPrefix)
  }

  def emptyPlasmaMap = new PlasmaMap[Array[Byte], Array[Byte]](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
  val emptyTreeErgoValue: ErgoValue[AvlTree] = emptyPlasmaMap.ergoValue
  val emptyTree: AvlTree = emptyTreeErgoValue.getValue

  val g: GroupElement = CGroupElement(CryptoConstants.dlogGroup.generator)

  val noteContract = scala.io.Source.fromFile("contracts/onchain/note.es", "utf-8").getLines.mkString("\n")

  val reserveContract = scala.io.Source.fromFile("contracts/onchain/reserve.es", "utf-8").getLines.mkString("\n")

  val receiptContract = scala.io.Source.fromFile("contracts/onchain/receipt.es", "utf-8").getLines.mkString("\n")

  val noteErgoTree = compile(noteContract)
  val noteAddress = getAddressFromErgoTree(noteErgoTree)

  val reserveErgoTree = compile(reserveContract)
  val reserveAddress = getAddressFromErgoTree(reserveErgoTree)

  val redemptionContract = scala.io.Source.fromFile("contracts/layer2/redemption.es", "utf-8").getLines.mkString("\n")
  val redemptionErgoTree = compile(redemptionContract)
  val redemptionAddress = getAddressFromErgoTree(redemptionErgoTree)

  val redemptionProducerContract = scala.io.Source.fromFile("contracts/layer2/redproducer.es", "utf-8").getLines.mkString("\n")
  val redemptionProducerErgoTree = compile(redemptionProducerContract)
  val redemptionProducerAddress = getAddressFromErgoTree(redemptionProducerErgoTree)
}

object Printer extends App {
  println("Redemption p2s address: " + Constants.redemptionAddress)
  println("Redemption producer p2s address: " + Constants.redemptionProducerAddress)
}
