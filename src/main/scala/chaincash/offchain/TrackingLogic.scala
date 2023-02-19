package chaincash.offchain

import org.ergoplatform.ErgoBox
import scorex.util.ModifierId
import scorex.util.encode.Base16
import special.sigma.GroupElement

object TrackingTypes {

  type ReserveNftId = ModifierId
  type NoteTokenId = ModifierId
  type UtxoId = ModifierId

  case class SigData(reserveId: ReserveNftId, valueBacked: Long, a: GroupElement, z: BigInt)
  case class NoteData(currentUtxo: ErgoBox, history: IndexedSeq[SigData])
  case class ReserveData(reserveBox: ErgoBox, signedUnspentNotes: IndexedSeq[UtxoId]) {
    def reserveNftId: ReserveNftId = ModifierId @@ Base16.encode(reserveBox.additionalTokens.toArray.head._1)
  }

}
