package chaincash

import chaincash.contracts.Constants
import chaincash.offchain.SigUtils
import com.google.common.primitives.Longs
import io.getblok.getblok_plasma.PlasmaParameters
import io.getblok.getblok_plasma.collections.{PlasmaMap, Proof}
import org.ergoplatform.P2PKAddress
import org.ergoplatform.appkit.impl.{ErgoScriptContract, ErgoTreeContract, OutBoxImpl}
import org.ergoplatform.appkit.{AppkitHelpers, BlockchainContext, ConstantsBuilder, ContextVar, ErgoValue, InputBox, NetworkType, OutBox, OutBoxBuilder, SignedTransaction}
import org.ergoplatform.sdk.ErgoToken
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import sigmastate.AvlTreeFlags
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval._
import sigmastate.serialization.GroupElementSerializer
import special.sigma.{AvlTree, GroupElement}

import collection.JavaConverters._
import java.util
import scala.util.Try

class ChainCashSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks with HttpClientTesting {

  implicit val addrEncoder = Constants.ergoAddressEncoder

  val fakeScript = "sigmaProp(true)"

  val emptyTree = Constants.emptyTree

  val noteTokenId = "4b2d8b7beb3eaac8234d9e61792d270898a43934d6a27275e4f3a044609c9f2a"

  val oracleNFT = "121A3A5250655368566D597133743677397A24432646294A404D635166546A57"
  val oracleNFTBytes = Base16.decode(oracleNFT).get

  val reserveNFT = "161A3A5250655368566D597133743677397A24432646294A404D635166546A57"
  val reserveNFTBytes = Base16.decode(reserveNFT).get

  val holderSecret = SigUtils.randBigInt
  val holderPk = Constants.g.exp(holderSecret.bigInteger)
  val changeAddress = P2PKAddress(ProveDlog(holderPk)).toString()

  val minValue = 1000000000L
  val feeValue = 1000000L

  val noteValue: Long = 1

  val fakeTxId1 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b809"
  val fakeTxId2 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b808"
  val fakeTxId3 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b807"
  val fakeTxId4 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b806"
  val fakeTxId5 = "f9e5ce5aa0d95f5d54a7bc89c46730d9662397067250aa18a0039631c0f5b105"
  val fakeIndex = 1.toShort

  def createOut(contract: String,
                value: Long,
                registers: Array[ErgoValue[_]],
                tokens: Array[ErgoToken])(implicit ctx: BlockchainContext): OutBoxImpl = {
    val c = ErgoScriptContract.create(new org.ergoplatform.appkit.Constants, contract, Constants.networkType)
    val ebc = AppkitHelpers.createBoxCandidate(value, c.getErgoTree, tokens, registers, ctx.getHeight)
    new OutBoxImpl(ebc)
  }

  private def addTokens(outBoxBuilder: OutBoxBuilder)(tokens: java.util.List[ErgoToken]) = {
    if (tokens.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.tokens(tokens.asScala: _*)
    }
  }

  private def addRegisters(
                            outBoxBuilder: OutBoxBuilder
                          )(registers: java.util.List[ErgoValue[_]]) = {
    if (registers.isEmpty) outBoxBuilder
    else {
      outBoxBuilder.registers(registers.asScala: _*)
    }
  }

  def getAddressFromString(string: String) =
    Try(Constants.ergoAddressEncoder.fromString(string).get).getOrElse(throw new Exception(s"Invalid address [$string]"))


  def decodeBigInt(encoded: String): BigInt = Try(BigInt(encoded, 10)).recover { case ex => BigInt(encoded, 16) }.get

  def createTx(
                inputBoxes: Array[InputBox],
                dataInputs: Array[InputBox],
                boxesToCreate: Array[OutBoxImpl],
                fee: Long,
                changeAddress: String,
                proveDlogSecrets: Array[String],
                broadcast: Boolean
              )(implicit ctx: BlockchainContext): SignedTransaction = {
    val txB = ctx.newTxBuilder
    val outputBoxes: Array[OutBox] = boxesToCreate.map { box =>
      val outBoxBuilder: OutBoxBuilder = txB
        .outBoxBuilder()
        .value(box.getValue())
        .creationHeight(box.getCreationHeight())
        .contract(new ErgoTreeContract(box.getErgoTree(), NetworkType.MAINNET))
      val outBoxBuilderWithTokens: OutBoxBuilder =
        addTokens(outBoxBuilder)(box.getTokens())
      val outBox: OutBox =
        addRegisters(outBoxBuilderWithTokens)(box.getRegisters()).build
      outBox
    }
    val inputs = new util.ArrayList[InputBox]()

    inputBoxes.foreach(inputs.add)

    val dataInputBoxes = new util.ArrayList[InputBox]()

    dataInputs.foreach(dataInputBoxes.add)

    val txToSign = ctx
      .newTxBuilder()
      .boxesToSpend(inputs)
      .withDataInputs(dataInputBoxes)
      .outputs(outputBoxes: _*)
      .fee(fee)
      .sendChangeTo(getAddressFromString(changeAddress))
      .build()

    val proveDlogSecretsBigInt = proveDlogSecrets.map(decodeBigInt)

    val dlogProver = proveDlogSecretsBigInt.foldLeft(ctx.newProverBuilder()) {
      case (oldProverBuilder, newDlogSecret) =>
        oldProverBuilder.withDLogSecret(newDlogSecret.bigInteger)
    }

    val signedTx = dlogProver.build().sign(txToSign)
    if (broadcast) ctx.sendTransaction(signedTx)
    signedTx
  }

  property("spending should work - no change") {
    createMockedErgoClient(MockData(Nil, Nil)).execute { implicit ctx: BlockchainContext =>

      val msg: Array[Byte] = Longs.toByteArray(noteValue) ++ Base16.decode(noteTokenId).get
      val sig = SigUtils.sign(msg, holderSecret)

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
          .registers(Constants.emptyTreeErgoValue, ErgoValue.of(holderPk))
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
          .registers(ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)

      val noteOutput = createOut(
        Constants.noteContract,
        minValue,
        Array(ErgoValue.of(outTree), ErgoValue.of(holderPk)),
        Array(new ErgoToken(noteTokenId, 1))
      )

      val inputs = Array[InputBox](noteInput)
      val dataInputs = Array[InputBox](reserveDataInput)
      val outputs = Array[OutBoxImpl](noteOutput)

      noException shouldBe thrownBy {
        createTx(
          inputs,
          dataInputs,
          outputs,
          fee = feeValue,
          changeAddress,
          Array[String](),
          false
        )
      }
    }
  }

  property("redemption should work") {
    createMockedErgoClient(MockData(Nil, Nil)).execute { implicit ctx: BlockchainContext =>
      val msg: Array[Byte] = Longs.toByteArray(noteValue) ++ Base16.decode(noteTokenId).get
      val sig = SigUtils.sign(msg, holderSecret)

      val plasmaMap = new PlasmaMap[Array[Byte], Array[Byte]](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
      val sigBytes = GroupElementSerializer.toBytes(sig._1) ++ sig._2.toByteArray
      val insertRes = plasmaMap.insert(reserveNFTBytes -> sigBytes)
      val _ = insertRes.proof
      val outTree = plasmaMap.ergoValue.getValue

      val lookupRes = plasmaMap.lookUp(reserveNFTBytes)
      val lookupProof = lookupRes.proof

      val noteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue + feeValue)
          .tokens(new ErgoToken(noteTokenId, noteValue))
          .registers(ErgoValue.of(outTree), ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(-1: Byte)),
            new ContextVar(2, ErgoValue.of(Array.emptyByteArray)),
            new ContextVar(3, ErgoValue.of(Array.emptyByteArray))
          )

      val reserveInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFTBytes, 1))
          .registers(ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(0: Byte)),
            new ContextVar(1, ErgoValue.of(lookupProof.bytes)),
            new ContextVar(2, ErgoValue.of(Longs.toByteArray(noteValue)))
          )

      val oracleRate = 500000L // nanoErg per mg
      val oracleDataInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(oracleNFTBytes, 1))
          .registers(ErgoValue.of(oracleRate * 1000000))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), "{false}"))
          .build()
          .convertToInputWith(fakeTxId3, fakeIndex)

      val reserveOutput = createOut(
        Constants.reserveContract,
        minValue - oracleRate * 98 / 100,
        registers = Array(ErgoValue.of(holderPk)),
        tokens = Array(new ErgoToken(reserveNFT, 1))
      )

      val inputs = Array[InputBox](noteInput, reserveInput)
      val dataInputs = Array[InputBox](oracleDataInput)
      val outputs = Array[OutBoxImpl](reserveOutput)

      createTx(
        inputs,
        dataInputs,
        outputs,
        fee = feeValue ,
        changeAddress,
        Array[String](holderSecret.toString()),
        false
      )
    }
  }

  property("spending should work - multiple notes and change") {
    createMockedErgoClient(MockData(Nil, Nil)).execute { implicit ctx: BlockchainContext =>

      val firstNoteTokenId = noteTokenId
      val secondNoteTokenId = Base16.encode(Blake2b256.apply(noteTokenId))

      val firstNoteValue = 55
      val secondNoteValue = 60

      val msg1: Array[Byte] = Longs.toByteArray(firstNoteValue) ++ Base16.decode(firstNoteTokenId).get
      val msg2: Array[Byte] = Longs.toByteArray(secondNoteValue) ++ Base16.decode(secondNoteTokenId).get
      val sig1 = SigUtils.sign(msg1, holderSecret)
      val sig2 = SigUtils.sign(msg2, holderSecret)

      def insertToEmptyTree(sig: (GroupElement, BigInt)): (Proof, AvlTree) = {
        val plasmaMap = new PlasmaMap[Array[Byte], Array[Byte]](AvlTreeFlags.InsertOnly, PlasmaParameters.default)
        val sigBytes = GroupElementSerializer.toBytes(sig._1) ++ sig._2.toByteArray
        val insertRes = plasmaMap.insert(reserveNFTBytes -> sigBytes)
        val insertProof = insertRes.proof
        val outTree = plasmaMap.ergoValue.getValue
        insertProof -> outTree
      }

      val (insertProof1, outTree1) = insertToEmptyTree(sig1)
      val (insertProof2, outTree2) = insertToEmptyTree(sig2)

      val firstNoteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(2 * minValue)
          .tokens(new ErgoToken(firstNoteTokenId, firstNoteValue))
          .registers(Constants.emptyTreeErgoValue, ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(0: Byte)),
            new ContextVar(1, ErgoValue.of(sig1._1)),
            new ContextVar(2, ErgoValue.of(sig1._2.toByteArray)),
            new ContextVar(3, ErgoValue.of(insertProof1.bytes)),
            new ContextVar(4, ErgoValue.of(2: Byte))
          )

      val secondNoteInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(2 * minValue + feeValue)
          .tokens(new ErgoToken(secondNoteTokenId, secondNoteValue))
          .registers(Constants.emptyTreeErgoValue, ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.noteContract))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(1: Byte)),
            new ContextVar(1, ErgoValue.of(sig2._1)),
            new ContextVar(2, ErgoValue.of(sig2._2.toByteArray)),
            new ContextVar(3, ErgoValue.of(insertProof2.bytes)),
            new ContextVar(4, ErgoValue.of(3: Byte))
          )

      val reserveDataInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFTBytes, 1))
          .registers(ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)

      val note1Output = createOut(
        Constants.noteContract,
        minValue,
        registers = Array(ErgoValue.of(outTree1), ErgoValue.of(holderPk)),
        tokens = Array(new ErgoToken(firstNoteTokenId, 50))
      )

      val note1Change = createOut(
        Constants.noteContract,
        minValue,
        registers = Array(ErgoValue.of(outTree1), ErgoValue.of(holderPk)),
        tokens = Array(new ErgoToken(firstNoteTokenId, 5))
      )

      val note2Output = createOut(
        Constants.noteContract,
        minValue,
        registers = Array(ErgoValue.of(outTree2), ErgoValue.of(holderPk)),
        tokens = Array(new ErgoToken(secondNoteTokenId, 50))
      )

      val note2Change = createOut(
        Constants.noteContract,
        minValue,
        registers = Array(ErgoValue.of(outTree2), ErgoValue.of(holderPk)),
        tokens = Array(new ErgoToken(secondNoteTokenId, 10))
      )

      val inputs = Array[InputBox](firstNoteInput, secondNoteInput)
      val dataInputs = Array[InputBox](reserveDataInput)
      val outputs = Array[OutBoxImpl](note1Output, note2Output, note1Change, note2Change)

      noException shouldBe thrownBy {
        createTx(
          inputs,
          dataInputs,
          outputs,
          fee = feeValue,
          changeAddress,
          Array[String](),
          false
        )
      }
    }
  }

  property("refund - init") {
    createMockedErgoClient(MockData(Nil, Nil)).execute { implicit ctx: BlockchainContext =>
      val reserveInput =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .tokens(new ErgoToken(reserveNFTBytes, 1))
          .registers(ErgoValue.of(holderPk))
          .contract(ctx.compileContract(ConstantsBuilder.empty(), Constants.reserveContract))
          .build()
          .convertToInputWith(fakeTxId2, fakeIndex)
          .withContextVars(
            new ContextVar(0, ErgoValue.of(2: Byte))
          )

      val fundingBox =
        ctx
          .newTxBuilder()
          .outBoxBuilder
          .value(minValue)
          .contract(ctx.compileContract(ConstantsBuilder.empty(), fakeScript))
          .build()
          .convertToInputWith(fakeTxId1, fakeIndex)

      val reserveOutput = createOut(
        Constants.reserveContract,
        minValue,
        registers = Array(ErgoValue.of(holderPk), ErgoValue.of(ctx.getHeight - 2)),
        tokens = Array(new ErgoToken(reserveNFT, 1))
      )

      val inputs = Array[InputBox](fundingBox, reserveInput)
      val dataInputs = Array[InputBox]()
      val outputs = Array[OutBoxImpl](reserveOutput)

      createTx(
        inputs,
        dataInputs,
        outputs,
        fee = feeValue ,
        changeAddress,
        Array[String](holderSecret.toString()),
        false
      )
    }
  }

}
