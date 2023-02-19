package chaincash.offchain

import io.circe.parser.parse
import org.ergoplatform.ErgoBox
import scorex.util.{ModifierId, ScorexLogging}
import scorex.util.encode.Base16
import TrackingTypes._

trait TrackingUtils extends WalletUtils with HttpUtils with ScorexLogging {

  val noteScanId = 21
  val reserveScanId = 20

  val heightKey = "height"

  def lastProcessedHeight(): Int = DbEntities.state.get(heightKey).map(_.toInt).getOrElse(0)

  def processReservesFor(from: Int, to: Int) = {

    def processBox(box: ErgoBox): Option[ReserveData] = {
      val boxId = box.id
      val boxTokens = box.additionalTokens.toArray
      if(boxTokens.isEmpty) {
        log.warn(s"Reserve box with no NFT: ${Base16.encode(boxId)}")
        return None
      }
      val reserveNftRecord = boxTokens.head
      if(reserveNftRecord._2 > 1) {
        log.warn(s"Reserve box with more than one id token: ${Base16.encode(boxId)}")
        return None
      }
      val reserveNftId = reserveNftRecord._1
      val reserveNftIdEncoded = ModifierId @@ Base16.encode(reserveNftId)
      DbEntities.reserves.get(reserveNftIdEncoded) match {
        case Some(reserveData: ReserveData) =>
          // if existing reserve box updated, e.g. top-up done on it
          Some(ReserveData(box, reserveData.signedUnspentNotes))
        case None =>
          Some(ReserveData(box, IndexedSeq.empty))
      }
    }

    val reserveScanUrl = s"$serverUrl/scan/unspentBoxes/$reserveScanId?minInclusionHeight=$from&maxInclusionHeight=$to"
    val boxesUnspentJson = parse(getJsonAsString(reserveScanUrl)).toOption.get
    val boxes = boxesUnspentJson.\\("box").map(_.as[ErgoBox].toOption.get)

    boxes.foreach{box =>
      processBox(box).map(rd => DbEntities.reserves.put(key = rd.reserveNftId, value = rd))
    }
    println("sz: " + boxes.size)
  }

  def processNotes(from: Int, to: Int) = {

  }

  def processBlocks() = {
    val localHeight = lastProcessedHeight()
    val nodeHeight = fetchNodeHeight()
    if (nodeHeight > localHeight) {
      processReservesFor(localHeight, nodeHeight)
      processNotes(localHeight, nodeHeight)
      DbEntities.state.put(heightKey, nodeHeight.toString)
    }
  }


}
