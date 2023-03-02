package chaincash.offchain

import TrackingTypes.NoteId
import sigmastate.interpreter.CryptoConstants.EcPointType

trait NotePredicate {
  /**
   * Whether a note with identifier `noteId` can be accepted
   */
  def acceptable(noteId: NoteId): Boolean

  def acceptable(noteIds: Seq[NoteId]): Boolean = {
    noteIds.map(acceptable).forall(_ == true)
  }
}

// todo: tests
class ExampleNotePredicate(whitelist: Set[EcPointType]) extends NotePredicate {

  /**
   * Whether a note with identifier `noteId` can be accepted
   */
  override def acceptable(noteId: NoteId): Boolean = {
    DbEntities.unspentNotes.get(noteId) match {
      case Some(nd) =>
        val holder = nd.holder
        // todo: estimate reserves
        whitelist.contains(holder)
      case None =>
        false
    }
  }

}