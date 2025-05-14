package gp

case class Note(noteId: Array[Byte], amount: Long, ownerPubKey: Array[Byte])

case class MintTransaction(output: Note)

object MintTransaction {
  val OraclePubKey: Array[Byte] = Array.fill(33)(0)
}
