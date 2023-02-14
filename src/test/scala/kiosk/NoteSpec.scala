package kiosk

import chaincash.contracts.{ChaincashSpec, Constants}
import kiosk.ergo.{DhtData, KioskAvlTree, KioskBoolean, KioskBox, KioskGroupElement, KioskInt, KioskLong}
import kiosk.script.ScriptUtil
import kiosk.tx.TxUtil
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ErgoToken, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import sigmastate.eval.CGroupElement

class ChainCashSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  val emptyTree = Constants.emptyTree
  val noteTokenId = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2a"

  val changeAddress = "9gQqZyxyjAptMbfW1Gydm3qaap11zd6X9DrABwgEE9eRdRvd27p"

  val changePK = ScriptUtil.ergoAddressEncoder.fromString(changeAddress).get.asInstanceOf[P2PKAddress].pubkey.value

  val changePKasGE = CGroupElement(changePK)

  val ergoClient = createMockedErgoClient(MockData(Nil, Nil))

  val minValue = 1000000000L
  val feeValue = 1000000L

  val fakeTxId1 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val fakeTxId2 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b808"
  val fakeTxId3 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b807"
  val fakeTxId4 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b806"
  val fakeTxId5 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b105"
  val fakeIndex = 1.toShort

  property("spending should work - no change") {
    ergoClient.execute { implicit ctx: BlockchainContext =>

      val noteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue + feeValue)
          .tokens(new ErgoToken(noteTokenId, 1))
          .registers(Constants.emptyTreeErgoValue, KioskGroupElement(changePKasGE).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId4, fakeIndex)

      val noteOutput = KioskBox(
        Constants.noteAddress,
        minValue,
        registers = Array(new KioskAvlTree(Constants.emptyTree), KioskGroupElement(changePKasGE)),
        tokens = Array((noteTokenId, 1))
      )

      val inputs = Array[InputBox](noteInput)
      val dataInputs = Array[InputBox]()
      val outputs = Array[KioskBox](noteOutput)
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
