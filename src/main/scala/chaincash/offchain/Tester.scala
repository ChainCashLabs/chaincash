package chaincash.offchain

object Tester extends App with ReserveUtils with NoteUtils {
  override val serverUrl: String = "http://127.0.0.1:9053"


  println(createNote(3))

}