package fi.spectrum.sim

import fi.spectrum.sim.runtime.NonRunnable
import org.ergoplatform.ErgoBox
import scorex.util.encode.Base16

import scala.util.Try

trait BoxSpec[+F[_]] {
  self =>
  val id: Coll[Byte]
  val value: Long
  val creationHeight: Int
  val tokens: Coll[(Coll[Byte], Long)]
  val registers: Map[Int, Any]
  val validatorBytes: String
  val validator: F[Boolean]
  val constants: Map[Int, Any] = Map.empty

  type Box = BoxSpec[NonRunnable]

  final def SELF: BoxSpec[F] = self

  final def creationInfo: (Int, Int) = (creationHeight, creationHeight)

  final def propositionBytes: Coll[Byte] = validatorBytes.getBytes().toVector

  final def getConstant[T](i: Int): Option[T] = constants.get(i).flatMap(c => Try(c.asInstanceOf[T]).toOption)

  final def setRegister[T](reg: Int, v: T): BoxSpec[F] =
    new BoxSpec[F] {
      override val id: Coll[Byte]                   = self.id
      override val value: Long                      = self.value
      override val creationHeight: Int              = self.creationHeight
      override val tokens: Coll[(Coll[Byte], Long)] = self.tokens
      override val registers: Map[Int, Any]         = self.registers + (reg -> v)
      override val validatorBytes: String           = self.validatorBytes
      override val validator: F[Boolean]            = self.validator
      override val constants: Map[Int, Any]         = self.constants
    }
}

// Non-runnable projection of a box.
final case class AnyBoxSpec(
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val validatorBytes: String,
  override val constants: Map[Int, Any]
) extends BoxSpec[NonRunnable] {
  override val validator: NonRunnable[Boolean] = ()
}

object AnyBoxSpec {
  implicit val tryFromBox: TryFromBox[BoxSpec, NonRunnable] =
    (bx: ErgoBox) =>
      Some(
        AnyBoxSpec(
          id             = bx.id.toVector,
          value          = bx.value,
          creationHeight = bx.creationHeight,
          validatorBytes = Base16.encode(bx.ergoTree.bytes),
          tokens         = bx.additionalTokens.toArray.map { case (id, v) => CollOpaque(id.toVector) -> v }.toVector,
          registers = bx.additionalRegisters.toVector.map { case (r, v) =>
            r.number.toInt -> sigma.transformVal(v)
          }.toMap,
          constants = bx.ergoTree.constants.toVector.zipWithIndex.map { case (c, ix) =>
            ix -> sigma.transformVal(c)
          }.toMap
        )
      )
}
