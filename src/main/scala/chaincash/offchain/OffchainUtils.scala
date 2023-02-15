package chaincash.offchain

import chaincash.contracts.Constants
import kiosk.ErgoUtil.randBigInt
import org.ergoplatform.P2PKAddress
import scorex.crypto.hash.Blake2b256
import sigmastate.eval.CGroupElement
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement
import sigmastate.eval._
import sigmastate.serialization.GroupElementSerializer

import scala.annotation.tailrec

object OffchainUtils {
  import Constants.g

  def sign(msg: Array[Byte], sk: BigInt): (GroupElement, BigInt) = {
    val r = randBigInt
    println(s"secret: ${r.toString(16)}")
    val g: GroupElement = CryptoConstants.dlogGroup.generator
    val a: GroupElement = g.exp(r.bigInteger)
    val z = (r + sk * BigInt(scorex.crypto.hash.Blake2b256(msg))) % CryptoConstants.groupOrder
    (a, z)
  }

  @tailrec
  def signOld(msg: Array[Byte], sk: BigInt): (BigInt, BigInt) = {
    val r = randBigInt
    println(s"secret: ${r.toString(16)}")
    val U: GroupElement = g.exp(r.bigInteger)
    val c = BigInt(Blake2b256.hash(GroupElementSerializer.toBytes(U) ++ msg))
    val s = r - c * sk
    if(c.bitLength <= 255 && s.bitLength <= 255) {
      (c, s)
    } else {
      println("Re-generating signature")
      signOld(msg, sk)
    }
  }

  def verifyOld(msg: Array[Byte], signature: (BigInt, BigInt), pk: GroupElement): Boolean = {
    val c = signature._1
    val s = signature._2
    val U = g.exp(s.bigInteger).multiply(pk.exp(c.bigInteger))
    c == BigInt(Blake2b256.hash(GroupElementSerializer.toBytes(U) ++ msg))
  }
}

object SigTester extends App {
  import OffchainUtils._
  import Constants.g

  val sk = randBigInt

  val msg = "hello".getBytes("UTF-8")

  val sig = sign(msg, sk)
  val pk = g.exp(sk.bigInteger)

 // println(verify(msg, sig, pk))
}
