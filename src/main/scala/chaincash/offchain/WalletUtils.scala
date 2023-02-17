package chaincash.offchain

import io.circe.parser.parse
import org.ergoplatform.{ErgoBox, ErgoBoxCandidate, ErgoScriptPredef, JsonCodecs}

trait WalletUtils extends JsonCodecs {
  val serverUrl: String

  val feeValue = 2000000

  def createFeeOut(creationHeight: Int): ErgoBoxCandidate = {
    new ErgoBoxCandidate(feeValue, ErgoScriptPredef.feeProposition(720), creationHeight) // 0.002 ERG
  }

  def fetchInputs(): Seq[ErgoBox] = {
    val boxesUnspentUrl = s"$serverUrl/wallet/boxes/unspent?minConfirmations=0&maxConfirmations=-1&minInclusionHeight=0&maxInclusionHeight=-1"
    val boxesUnspentJson = parse(HttpUtils.getJsonAsString(boxesUnspentUrl)).toOption.get

    boxesUnspentJson.\\("box").map(_.as[ErgoBox].toOption.get)
  }

}
