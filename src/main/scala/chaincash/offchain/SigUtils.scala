package chaincash.offchain

import kiosk.ErgoUtil.randBigInt
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement
import sigmastate.eval._
import scala.annotation.tailrec

object SigUtils {

  @tailrec
  def sign(msg: Array[Byte], secretKey: BigInt): (GroupElement, BigInt) = {
    val r = randBigInt
    val g: GroupElement = CryptoConstants.dlogGroup.generator
    val a: GroupElement = g.exp(r.bigInteger)
    val z = (r + secretKey * BigInt(scorex.crypto.hash.Blake2b256(msg))) % CryptoConstants.groupOrder

    if(z.bitLength <= 255) {
      (a, z)
    } else {
      sign(msg,secretKey)
    }
  }

}