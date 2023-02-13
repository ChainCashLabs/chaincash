{
    // ERG variant

    // Data:
    //  - token #0 - identifying singleton token
    //
    // Actions:
    //  - redeem note
    //  - top up
    //  - unlock refund, lock refund, refund

    val selfOut = OUTPUTS(0)
    val selfPreserved = selfOut.propositionBytes == SELF.propositionBytes && selfOut.tokens == SELF.tokens

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
      val oracleRate = goldOracle.R4[Long].get / 1000000 // normalize to nanoerg per mg of gold
      val tokensRedeemed = noteInput.tokens(0)._2 // 1 token == 1 mg of gold
      val nanoergsToRedeem = tokensRedeemed * oracleRate
      val redeemCorrect = (SELF.value - selfOut.value) <= nanoergsToRedeem

      val proof = getVar[Coll[Byte]](1).get
      val value = history.get(reserveId, proof).get

      val cBytes = value.slice(0, 32)
      val sBytes = value.slice(32, 64)
      val c = byteArrayToBigInt(cBytes)
      val s = byteArrayToBigInt(sBytes)

      val maxValueBytes = getVar[Coll[Byte]](2).get
      val message = maxValueBytes ++ noteTokenId
      val maxValue = byteArrayToLong(maxValueBytes)

      val reservePubKey = getVar[GroupElement](3).get

      val U = g.exp(s).multiply(reservePubKey.exp(c)).getEncoded // as a byte array

      val properSignature =
        (cBytes == blake2b256(U ++ message)) &&
            SELF.propositionBytes == proveDlog(reservePubKey).propBytes &&
            noteValue <= maxValue

      sigmaProp(selfPreserved && redeemCorrect && properSignature)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      // todo: implement refund
      sigmaProp(false)
    }



}