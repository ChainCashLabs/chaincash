package kiosk

import chaincash.contracts.{ChaincashSpec, Constants}
import chaincash.offchain.OffchainUtils
import com.google.common.primitives.Longs
import kiosk.ErgoUtil.randBigInt
import kiosk.ergo.{DhtData, KioskAvlTree, KioskBoolean, KioskBox, KioskGroupElement, KioskInt, KioskLong}
import kiosk.script.ScriptUtil
import kiosk.tx.TxUtil
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ContextVar, ErgoToken, ErgoValue, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.CGroupElement
import sigmastate.eval._

class ChainCashSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  implicit val addrEncoder = ScriptUtil.ergoAddressEncoder

  val emptyTree = Constants.emptyTree

  val noteTokenId = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2a"

  val reserveNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57"

  val holderSecret = randBigInt
  val holderPk = Constants.g.exp(holderSecret.bigInteger)
  val changeAddress = P2PKAddress(ProveDlog(holderPk)).toString()

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val minValue = 1000000000L
  val feeValue = 1000000L

  val noteValue: Long = 1

  val fakeTxId1 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val fakeTxId2 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b808"
  val fakeTxId3 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b807"
  val fakeTxId4 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b806"
  val fakeTxId5 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b105"
  val fakeIndex = 1.toShort

  property("spending should work - no change") {
    ergoClient.execute { implicit ctx: BlockchainContext =>

      val msg: Array[Byte] = Longs.toByteArray(noteValue) ++ Base16.decode(noteTokenId).get
      val sig = OffchainUtils.sign(msg, holderSecret)

      val noteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue + feeValue)
          .tokens(new ErgoToken(noteTokenId, noteValue))
          .registers(Constants.emptyTreeErgoValue, KioskGroupElement(holderPk).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(0: Byte)),
            new ContextVar(1, ErgoValue.of(sig._1)),
            new ContextVar(2, ErgoValue.of(sig._2.toByteArray))
          )

      val reserveDataInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFT, 1))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)

      val noteOutput = KioskBox(
        Constants.noteAddress,
        minValue,
        registers = Array(new KioskAvlTree(Constants.emptyTree), KioskGroupElement(holderPk)),
        tokens = Array((noteTokenId, 1))
      )

      val inputs = Array[InputBox](noteInput)
      val dataInputs = Array[InputBox](reserveDataInput)
      val outputs = Array[KioskBox](noteOutput)

      noException shouldBe thrownBy {
        TxUtil.createTx(
          inputs,
          dataInputs,
          outputs,
          fee = feeValue,
          changeAddress,
          Array[String](),
          Array[DhtData](),
          false
        )
      }
    }
  }

}
