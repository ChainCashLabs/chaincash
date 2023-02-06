package chaincash

import scala.util.Random

case class PubKey(value: String)

case class Issuer(pk: PubKey)

case class Signature(pk: PubKey)

trait Reserve {
  val issuer: Issuer

  def usCentAmount: Long // in us cents
}

trait TrustBasedReserve

trait CollateralBasedReserve extends Reserve

case class ErgReserve(nanoergs: Long, issuer: Issuer) extends CollateralBasedReserve {
  override def usCentAmount = ???
}

case class UsdReserve(usCentAmount: Long, issuer: Issuer) extends CollateralBasedReserve {
  val usd: Double = usCentAmount / 100.0
}

case class BackingStatement(reserve: Reserve, signature: Signature)

// amount is in usd cents for now
case class Note(id: Long, amount: Long, backing: Seq[BackingStatement])

object Note {
  def apply(amount: Long, backing: Seq[BackingStatement]): Note = {
    Note(Random.nextLong(), amount, backing)
  }
}

trait Transaction

case class ExchangeTransaction(input: Note, outputs: Seq[Note]) extends Transaction

object ExchangeTransaction {
  def payment(from: Note, spenderReserve: Reserve, amount: Long): ExchangeTransaction = {
    require(from.amount >= amount, "Input can't cover the payment")
    val changeOutputOpt = if (from.amount == amount) {
      None
    } else { // > case
      Some(Note(from.amount - amount, from.backing))
    }
    val newBackingStatement = BackingStatement(spenderReserve, Signature(spenderReserve.issuer.pk))
    val paymentOutput = Note(amount, backing = from.backing ++ Seq(newBackingStatement))
    ExchangeTransaction(from, paymentOutput +: changeOutputOpt.toSeq)
  }
}

case class MintingTransaction(output: Note) extends Transaction
case class RedeemingTransaction(input: Note) extends Transaction


trait AcceptanceFilter {
  def acceptable(note: Note): Boolean
}

case class UtxoSet(set: Seq[Note]){

  // inefficient and insecure
  def process(transaction: Transaction): UtxoSet = transaction match {
    case ExchangeTransaction(input, outputs) =>
      UtxoSet(set.filter(n => n != input) ++ outputs)
    case MintingTransaction(output) =>
      UtxoSet(set :+ output)
    case RedeemingTransaction(input) =>
      UtxoSet(set.filter(n => n != input))
  }
}

object UtxoSet {
  def empty: UtxoSet = UtxoSet(Seq.empty)
}

object Tester extends App {
  def testingAcceptanceFilter(utxoSet: UtxoSet): AcceptanceFilter = testingAcceptanceFilter(utxoSet.set)

  def testingAcceptanceFilter(circulatingNotes: Seq[Note]): AcceptanceFilter = {
    (note: Note) => {
      val noteReserves = note.backing.distinct
      val notesIntersecting = noteReserves.flatMap { r => circulatingNotes.filter(n => n.backing.contains(r)) }
      val notesIntersectingBacking = notesIntersecting.flatMap(_.backing.map(_.reserve).distinct).map(_.usCentAmount).sum
      notesIntersectingBacking > notesIntersecting.map(_.amount).sum
    }
  }

  val pk1 = PubKey("1")

  val issuer1 = Issuer(pk1)
  val issuer2 = Issuer(PubKey("2"))
  val issuer3 = Issuer(PubKey("3"))

  val reserve1 = UsdReserve(1000 * 100, issuer1)
  val reserve2 = UsdReserve(1500 * 100, issuer2)
  val reserve3 = UsdReserve(700 * 100, issuer3)

  var utxoSet = UtxoSet.empty

  val tx1 = MintingTransaction(Note(1, 100 * 100, Seq(BackingStatement(reserve1, Signature(pk1)))))

  utxoSet = utxoSet.process(tx1)

  val filter1 = testingAcceptanceFilter(utxoSet)
  println(filter1.acceptable(utxoSet.set.head))

}