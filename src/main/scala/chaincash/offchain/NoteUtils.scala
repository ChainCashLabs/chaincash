package chaincash.offchain

import chaincash.contracts.Constants
import chaincash.contracts.Constants.{noteErgoTree, reserveErgoTree}
import io.circe.syntax.EncoderOps
import org.ergoplatform.ErgoBox.{R4, R5}
import org.ergoplatform.{ErgoBoxCandidate, P2PKAddress, UnsignedErgoLikeTransaction, UnsignedInput}
import scorex.crypto.hash.Digest32
import sigmastate.Values.{AvlTreeConstant, GroupElementConstant}
import sigmastate.eval.Colls
import sigmastate.interpreter.ContextExtension
import sigmastate.eval._
import special.sigma.GroupElement

trait NoteUtils extends WalletUtils {
  // create note with nominal of `amountMg` mg of gold
  def createNote(amountMg: Long, ownerPubkey: GroupElement, changeAddress: P2PKAddress): Unit = {
    val inputs = fetchInputs().take(60)
    val creationHeight = inputs.map(_.creationHeight).max
    val noteTokenId = Digest32 @@ inputs.head.id

    val inputValue = inputs.map(_.value).sum
    require(inputValue >= feeValue * 21)

    val noteAmount = feeValue * 20

    val noteOut = new ErgoBoxCandidate(
      noteAmount,
      noteErgoTree,
      creationHeight,
      Colls.fromItems(noteTokenId -> amountMg),
      Map(R4 -> AvlTreeConstant(Constants.emptyTree), R5 -> GroupElementConstant(ownerPubkey))
    )
    val feeOut = createFeeOut(creationHeight)
    val changeOutOpt = if(inputValue > 21 * feeValue) {
      val changeValue = inputValue - (21 * feeValue)
      Some(new ErgoBoxCandidate(changeValue, changeAddress.script, creationHeight))
    } else {
      None
    }

    val unsignedInputs = inputs.map(box => new UnsignedInput(box.id, ContextExtension.empty))
    val outs = Seq(noteOut, feeOut) ++ changeOutOpt.toSeq
    val tx = new UnsignedErgoLikeTransaction(unsignedInputs.toIndexedSeq, IndexedSeq.empty, outs.toIndexedSeq)
    println(tx.asJson)
  }

  def createNote(amountMg: Long): Unit = {
    val changeAddress = fetchChangeAddress()
    createNote(amountMg, changeAddress.pubkey.value, changeAddress)
  }

}
