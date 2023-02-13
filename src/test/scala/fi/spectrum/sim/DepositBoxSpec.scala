package fi.spectrum.sim

import fi.spectrum.sim.RuntimeState._
import fi.spectrum.sim.runtime.NonRunnable
import fi.spectrum.sim.syntax._

// 1. Declare the box holding validator you want to simulate:
final class DepositBoxSpec[F[_]: RuntimeState](
  override val id: Coll[Byte],
  override val value: Long,
  override val creationHeight: Int,
  override val tokens: Coll[(Coll[Byte], Long)],
  override val registers: Map[Int, Any],
  override val constants: Map[Int, Any],
  override val validatorBytes: String
) extends BoxSpec[F] {

  override val validator: F[Boolean] =
    withRuntimeState { implicit ctx =>
      // Bind constants:
      val PoolId: Coll[Byte]         = getConstant(1).get
      val RedeemerProp: Coll[Byte]   = getConstant(3).get
      val RefundPk: Boolean          = getConstant(6).get
      val BundlePropHash: Coll[Byte] = getConstant(10).get
      val ExpectedNumEpochs: Int     = getConstant(14).get
      val MinerPropBytes: Coll[Byte] = getConstant(18).get
      val MaxMinerFee: Long          = getConstant(21).get

      // 2. Copy-paste relevant validator (In our case we pasted Deposit validator here).
      // You can set breakpoints below:

      // ===== Getting SELF data ===== //
      val deposit = SELF.tokens(0)

      // ===== Getting INPUTS data ===== //
      val poolIn      = INPUTS(0)
      val bundleKeyId = poolIn.id

      // ===== Getting OUTPUTS data ===== //
      val redeemerOut = OUTPUTS(1) // 0 -> pool_out, 1 -> redeemer_out, 2 -> bundle_out
      val bundleOut   = OUTPUTS(2)

      // ===== Calculations ===== //
      val expectedVLQ       = deposit._2
      val expectedNumEpochs = ExpectedNumEpochs
      val expectedTMP       = expectedVLQ * expectedNumEpochs

      // ===== Validating conditions ===== //
      // 1.
      val validPoolIn = poolIn.tokens(0)._1 == PoolId
      // 2.
      val validRedeemerOut =
        redeemerOut.propositionBytes == RedeemerProp &&
        (bundleKeyId, 0x7fffffffffffffffL - 1L) == redeemerOut.tokens(0)
      // 3.
      val validBundle = {
        blake2b256(bundleOut.propositionBytes) == BundlePropHash &&
        bundleOut.R4[Coll[Byte]].get == RedeemerProp &&
        bundleOut.R5[Coll[Byte]].get == PoolId &&
        (poolIn.tokens(3)._1, expectedVLQ) == bundleOut.tokens(0) &&
        (poolIn.tokens(4)._1, expectedTMP) == bundleOut.tokens(1) &&
        (bundleKeyId, 1L) == bundleOut.tokens(2)
      }

      // 4.
      val validMinerFee = OUTPUTS
        .map { (o: Box) =>
          if (o.propositionBytes == MinerPropBytes) o.value else 0L
        }
        .fold(0L, (a: Long, b: Long) => a + b) <= MaxMinerFee

      sigmaProp(RefundPk || (validPoolIn && validRedeemerOut && validBundle && validMinerFee))
    }
}

object DepositBoxSpec {
  def apply[F[_]: RuntimeState, G[_]](bx: BoxSpec[G]): DepositBoxSpec[F] =
    new DepositBoxSpec(bx.id, bx.value, bx.creationHeight, bx.tokens, bx.registers, bx.constants, bx.validatorBytes)
  implicit def tryFromBox[F[_]: RuntimeState]: TryFromBox[DepositBoxSpec, F] =
    AnyBoxSpec.tryFromBox.translate(apply[F, NonRunnable])
}
