package chaincash.offchain

import io.circe.parser.parse
import org.ergoplatform.{ErgoAddressEncoder, ErgoBox, ErgoBoxCandidate, ErgoScriptPredef, JsonCodecs, P2PKAddress}

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

  def fetchChangeAddress(): P2PKAddress = {
    val boxesUnspentUrl = s"$serverUrl/wallet/status"
    val statusJson = parse(HttpUtils.getJsonAsString(boxesUnspentUrl)).toOption.get
    val addrStrOpt = statusJson.asObject.get.apply("changeAddress").flatMap(_.asString)
    val eae = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
    val addrOpt = addrStrOpt.map(s => eae.fromString(s).get.asInstanceOf[P2PKAddress])
    addrOpt.get
  }

}
