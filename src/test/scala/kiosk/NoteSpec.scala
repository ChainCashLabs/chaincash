package kiosk

import chaincash.contracts.{ChaincashSpec, Constants}
import chaincash.offchain.OffchainUtils
import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.PlasmaMap
import kiosk.ErgoUtil.randBigInt
import kiosk.ergo.{DhtData, KioskAvlTree, KioskBoolean, KioskBox, KioskGroupElement, KioskInt, KioskLong}
import kiosk.script.ScriptUtil
import kiosk.tx.TxUtil
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.{BlockchainContext, ConstantsBuilder, ContextVar, ErgoId, ErgoToken, ErgoValue, HttpClientTesting, InputBox}
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.encode.{Base58, Base64}
import scorex.util.encode.Base16
import sigmastate.{AvlTreeFlags, Values}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval._
import sigmastate.serialization.GroupElementSerializer

class ChainCashSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  implicit val addrEncoder = ScriptUtil.ergoAddressEncoder

  val emptyTree = Constants.emptyTree

  val noteTokenId = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2a"

  val oracleNFT = "121A3A5250655368566D597133743677397A24432646294A404D635166546A57"
  val oracleNFTBytes = Base16.decode(oracleNFT).get

  val reserveNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57"
  val reserveNFTBytes = Base16.decode(reserveNFT).get

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

      val plasmaMap = new PlasmaMap[Array[Byte], Array[Byte]](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
      val sigBytes = GroupElementSerializer.toBytes(sig._1) ++ sig._2.toByteArray
      val insertRes = plasmaMap.insert(reserveNFTBytes -> sigBytes)
      val insertProof = insertRes.proof
      val outTree = plasmaMap.ergoValue.getValue

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
            new ContextVar(2, ErgoValue.of(sig._2.toByteArray)),
            new ContextVar(3, ErgoValue.of(insertProof.bytes))
          )

      val reserveDataInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFTBytes, 1))
          .registers(KioskGroupElement(holderPk).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)

      val noteOutput = KioskBox(
        Constants.noteAddress,
        minValue,
        registers = Array(new KioskAvlTree(outTree), KioskGroupElement(holderPk)),
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

  property("redemption should work") {
    ergoClient.execute { implicit ctx: BlockchainContext =>
      val msg: Array[Byte] = Longs.toByteArray(noteValue) ++ Base16.decode(noteTokenId).get
      val sig = OffchainUtils.sign(msg, holderSecret)

      val plasmaMap = new PlasmaMap[Array[Byte], Array[Byte]](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
      val sigBytes = GroupElementSerializer.toBytes(sig._1) ++ sig._2.toByteArray
      val insertRes = plasmaMap.insert(reserveNFTBytes -> sigBytes)
      val insertProof = insertRes.proof
      val outTree = plasmaMap.ergoValue.getValue

      val noteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue + feeValue)
          .tokens(new ErgoToken(noteTokenId, noteValue))
          .registers(new KioskAvlTree(outTree).getErgoValue, KioskGroupElement(holderPk).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(1: Byte)),
            new ContextVar(1, ErgoValue.of(sig._1)),
            new ContextVar(2, ErgoValue.of(sig._2.toByteArray)),
            new ContextVar(3, ErgoValue.of(insertProof.bytes))
          )

      val reserveInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFTBytes, 1))
          .registers(new KioskAvlTree(outTree).getErgoValue, KioskGroupElement(holderPk).getErgoValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(1: Byte)),
            new ContextVar(1, ErgoValue.of(sig._1)),
            new ContextVar(2, ErgoValue.of(sig._2.toByteArray)),
            new ContextVar(3, ErgoValue.of(insertProof.bytes))
          )

      val oracleDataInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(oracleNFTBytes, 1))
          .registers(ErgoValue.of(500000L))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), "{false}"))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val reserveOutput = KioskBox(
        Constants.reserveAddress,
        minValue,
        registers = Array(KioskGroupElement(holderPk)),
        tokens = Array((reserveNFT, 1))
      )

      val inputs = Array[InputBox](noteInput, reserveInput)
      val dataInputs = Array[InputBox](oracleDataInput)
      val outputs = Array[KioskBox](reserveOutput)

      TxUtil.createTx(
        inputs,
        dataInputs,
        outputs,
        fee = feeValue,
        changeAddress,
        Array[String](holderSecret.toString()),
        Array[DhtData](),
        false
      )
    }
  }

}
