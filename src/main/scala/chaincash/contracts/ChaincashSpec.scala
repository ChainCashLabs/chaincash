package chaincash.contracts

import kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import kiosk.ergo.KioskType
import kiosk.script.ScriptUtil
import kiosk.script.ScriptUtil.compiler
import org.ergoplatform.ErgoAddressEncoder.MainnetNetworkPrefix
import sigmastate.Values.ErgoTree
import sigmastate.eval.CompiletimeIRContext
import sigmastate.lang.{CompilerSettings, SigmaCompiler, TransformingSigmaBuilder}

object ChaincashSpec extends App {

  def compileV5(env: Map[String, KioskType[_]], ergoScript: String): ErgoTree = {
    import sigmastate.lang.Terms._
    implicit val irContext = new CompiletimeIRContext
    val networkPrefix = MainnetNetworkPrefix
    val compiler = SigmaCompiler(CompilerSettings(networkPrefix, TransformingSigmaBuilder, lowerMethodCalls = true))
    compiler.compile(env.mapValues(_.value), ergoScript).buildTree.toSigmaProp
  }

  val noteContract = scala.io.Source.fromFile("contracts/note.es", "utf-8").getLines.mkString("\n")

  val reserveContract = scala.io.Source.fromFile("contracts/reserve.es", "utf-8").getLines.mkString("\n")

  val noteErgoTree = compileV5(Map.empty, noteContract)
  val noteAddress = getStringFromAddress(getAddressFromErgoTree(noteErgoTree))
  println("Note contract address: " + noteAddress)

  val reserveErgoTree = compileV5(Map.empty, reserveContract)
  val reserveAddress = getStringFromAddress(getAddressFromErgoTree(reserveErgoTree))
  println("Reserve contract address: " + reserveAddress)

}
