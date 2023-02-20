package chaincash.offchain

import org.ergoplatform.ErgoBox
import org.ergoplatform.ErgoBox.R5
import scorex.util.ModifierId
import scorex.util.encode.Base16
import sigmastate.Values.GroupElementConstant
import sigmastate.eval.CGroupElement
import sigmastate.interpreter.CryptoConstants.EcPointType
import special.sigma.GroupElement

object TrackingTypes {

  type ReserveNftId = ModifierId
  type NoteTokenId = ModifierId
  type UtxoId = ModifierId

  case class SigData(reserveId: ReserveNftId, valueBacked: Long, a: GroupElement, z: BigInt)
  case class NoteData(currentUtxo: ErgoBox, history: IndexedSeq[SigData]) {
    def holder: EcPointType = currentUtxo.get(R5).get.asInstanceOf[GroupElementConstant].value.asInstanceOf[CGroupElement].wrappedValue
  }
  case class ReserveData(reserveBox: ErgoBox, signedUnspentNotes: IndexedSeq[UtxoId]) {
    def reserveNftId: ReserveNftId = ModifierId @@ Base16.encode(reserveBox.additionalTokens.toArray.head._1)
  }

}
