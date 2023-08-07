{
    // ERG variant

    // Data:
    //  - token #0 - identifying singleton token
    //  - R4 - signing key (as a group element)
    //
    // Actions:
    //  - redeem note
    //  - top up
    //  - init refund, cancel refund, complete refund

    val ownerKey = SELF.R4[GroupElement].get // used in notes and unlock/lock/refund actions
    val selfOut = OUTPUTS(0)
    val selfPreserved =
            selfOut.propositionBytes == SELF.propositionBytes &&
            selfOut.tokens == SELF.tokens &&
            selfOut.R4[GroupElement].get == SELF.R4[GroupElement].get

    val action = getVar[Byte](0).get

    if (action == 0) {
      // redeem path
      // provides gold price in nanoErg per kg

      val g: GroupElement = groupGenerator

      val noteInput = INPUTS(0)
      val noteTokenId = noteInput.tokens(0)._1
      val noteValue = noteInput.tokens(0)._2
      val history = noteInput.R4[AvlTree].get
      val reserveId = SELF.tokens(0)._1

      val goldOracle = CONTEXT.dataInputs(0)
      val properOracle = goldOracle.tokens(0)._1 == fromBase58("2DfY1K4rW9zPVaQgaDp2KXgnErjxKPbbKF5mq1851MJE")
      val oracleRate = goldOracle.R4[Long].get / 1000000 // normalize to nanoerg per mg of gold
      val tokensRedeemed = noteInput.tokens(0)._2 // 1 token == 1 mg of gold

      // 2% redemption fee
      val nanoergsToRedeem = tokensRedeemed * oracleRate * 98 / 100
      val redeemCorrect = (SELF.value - selfOut.value) <= nanoergsToRedeem

      val proof = getVar[Coll[Byte]](1).get
      val value = history.get(reserveId, proof).get

      val aBytes = value.slice(0, 33)
      val zBytes = value.slice(33, value.size)
      val a = decodePoint(aBytes)
      val z = byteArrayToBigInt(zBytes)

      val maxValueBytes = getVar[Coll[Byte]](2).get
      val message = maxValueBytes ++ noteTokenId
      val maxValue = byteArrayToLong(maxValueBytes)

      // Computing challenge
      val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
      val eInt = byteArrayToBigInt(e) // challenge as big integer

      // Signature is valid if g^z = a * x^e
      val properSignature = (g.exp(z) == a.multiply(ownerKey.exp(eInt))) &&
                             noteValue <= maxValue

      sigmaProp(selfPreserved && redeemCorrect && properSignature && properOracle)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      // todo: implement refund
      sigmaProp(false)
    }



}