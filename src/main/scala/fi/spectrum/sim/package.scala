package fi.spectrum

import org.ergoplatform.ErgoBox

package object sim {

  trait ToLedger[A, F[_]] {
    def toLedger(a: A): BoxSpec[F]
  }

  object ToLedger {
    implicit def apply[A, F[_]](implicit ev: ToLedger[A, F]): ToLedger[A, F] = ev

    implicit class ToLedgerOps[A](a: A) {
      def toLedger[F[_]](implicit ev: ToLedger[A, F]): BoxSpec[F] = ev.toLedger(a)
    }
  }

  trait TryFromBox[Box[_[_]], F[_]] {
    self =>
    def tryFromBox(bx: ErgoBox): Option[Box[F]]

    final def translate[ToBox[_[_]], G[_]](fk: Box[F] => ToBox[G]): TryFromBox[ToBox, G] =
      (bx: ErgoBox) => self.tryFromBox(bx).map(fk)
  }

  object TryFromBox {
    implicit class TryFromBoxOps[Box[+_[_]], F[_]](a: Box[F]) {
      def tryFromBox(bx: ErgoBox)(implicit ev: TryFromBox[Box, F]): Option[Box[F]] = ev.tryFromBox(bx)
    }
  }

  final case class CollOpaque[+A](inner: Vector[A]) {
    def fold[A1 >: A](z: A1, op: (A1, A1) => A1): A1 = inner.fold(z)(op)

    def map[B](f: A => B): CollOpaque[B] = inner.map(f)

    def apply(i: Int): A = inner.apply(i)

    def size: Int = inner.size

    override def toString: String = s"Coll(${inner.map(x => x.toString).mkString(", ")})"
  }

  implicit def toColl[A](vec: Vector[A]): Coll[A] = CollOpaque(vec)

  type Coll[A] = CollOpaque[A]

  object Coll {
    def apply[A](elems: A*): Coll[A] = CollOpaque(Vector.apply(elems: _*))
  }
}
