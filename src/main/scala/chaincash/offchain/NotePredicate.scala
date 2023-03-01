package chaincash.offchain

import TrackingTypes.NoteId

trait NotePredicate {
  /**
   * Whether a note with identifier `noteId` can be accepted
   */
  def acceptable(noteId: NoteId): Boolean

  def acceptable(noteIds: Seq[NoteId]): Boolean = {
    noteIds.map(acceptable).forall(_ == true)
  }
}
