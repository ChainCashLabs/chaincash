{
    // ERG variant

    // Data:
    //  - token #0 - identifying singleton token
    //  - R4 - signing key (as a group element)
    //  - R5 - refund init height (None if not set)
    //  - R6 - amount to refund (None if not set)
    //
    // Actions:
    //  - redeem note (#0)
    //  - top up      (#1)
    //  - init refund (#2)
    //  - cancel refund (#3)
    //  - complete refund (#4)

    val v = getVar[Byte](0).get
    val action = v / 10
    val index = v % 10

    val ownerKey = SELF.R4[GroupElement].get // reserve owner's key, used in notes and unlock/lock/refund actions
    val selfOut = OUTPUTS(index)
    val selfPreserved =
            selfOut.propositionBytes == SELF.propositionBytes &&
            selfOut.tokens == SELF.tokens &&
            selfOut.R4[GroupElement].get == SELF.R4[GroupElement].get

    if (action == 0) {
      // redemption path

      // OUTPUTS:
      // #1 - receipt
      // #2 - buyback

      val g: GroupElement = groupGenerator

      val receiptMode = getVar[Boolean](4).get

      // read note data if receiptMode == false, receipt data otherwise
      val noteInput = INPUTS(index)
      val noteTokenId = noteInput.tokens(0)._1
      val noteValue = noteInput.tokens(0)._2 // 1 token == 1 mg of gold
      val history = noteInput.R4[AvlTree].get
      val reserveId = SELF.tokens(0)._1

      // oracle provides gold price in nanoErg per kg in its R4 register
      val goldOracle = CONTEXT.dataInputs(0)
      // todo: externalize oracle NFT id
      val properOracle = goldOracle.tokens(0)._1 == fromBase58("2DfY1K4rW9zPVaQgaDp2KXgnErjxKPbbKF5mq1851MJE")
      val oracleRate = goldOracle.R4[Long].get / 1000000 // normalize to nanoerg per mg of gold

      // 2% redemption fee
      val maxToRedeem = noteValue * oracleRate * 98 / 100
      val redeemed = SELF.value - selfOut.value

      val buyBackCorrect = if (redeemed > 0) {
        val toOracle = redeemed * 2 / 1000
        val buyBackNFTId = fromBase64("EZoGigEZZw3opdJGfaM99XKQPGSqp7bqTJZo7wz+AyU=")
        val buyBackInput = INPUTS(2)
        val buyBackOutput = OUTPUTS(2)

        buyBackInput.tokens(0)._1 == buyBackNFTId &&
            buyBackOutput.tokens(0)._1 == buyBackNFTId &&
            (buyBackOutput.value - buyBackInput.value) >= toOracle
      } else {
        true
      }
      val redeemCorrect = (redeemed <= maxToRedeem) && buyBackCorrect

      val proof = getVar[Coll[Byte]](1).get
      val value = history.get(reserveId, proof).get

      val aBytes = value.slice(0, 33)
      val zBytes = value.slice(33, value.size)
      val a = decodePoint(aBytes)
      val z = byteArrayToBigInt(zBytes)

      val maxValueBytes = getVar[Coll[Byte]](2).get
      val position = getVar[Long](3).get
      val message = longToByteArray(position) ++ maxValueBytes ++ noteTokenId
      val maxValue = byteArrayToLong(maxValueBytes)

      // Computing challenge
      val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
      val eInt = byteArrayToBigInt(e) // challenge as big integer

      // Signature is valid if g^z = a * x^e
      val properSignature = (g.exp(z) == a.multiply(ownerKey.exp(eInt))) &&
                             noteValue <= maxValue

      // we check that receipt is properly formed, but we do not check receipt's contract here,
      // to avoid circular dependency as receipt contract depends on (hash of) our contract,
      // thus we are checking receipt contract in note and receipt contracts
      val receiptOutIndex = if (redeemed == 0) {
         getVar[Int](5).get
      } else {
         1
      }
      val receiptOut = OUTPUTS(receiptOutIndex)
      val properReceipt =
        receiptOut.tokens(0) == noteInput.tokens(0) &&
        receiptOut.R4[AvlTree].get == history  &&
        receiptOut.R5[Long].get == position    &&
        receiptOut.R6[Int].get >= HEIGHT - 20  &&  // 20 blocks for inclusion
        receiptOut.R6[Int].get <= HEIGHT &&
        receiptOut.R7[GroupElement].get == ownerKey

      // todo: could this be checked all the time ? if so then receiptMode can be eliminated
      val positionCorrect = if (receiptMode) {
        position < noteInput.R5[Long].get
      } else {
        true
      }

      sigmaProp(selfPreserved && properOracle && redeemCorrect && properSignature && properReceipt && positionCorrect)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      // todo: write tests for refund paths, document them
      if (action == 2) {
        // init refund
        val correctHeight = selfOut.R5[Int].get >= HEIGHT - 5
        val correctValue = selfOut.value >= SELF.value
        sigmaProp(selfPreserved && correctHeight && correctValue) && proveDlog(ownerKey)
      } else if (action == 3) {
        // cancel refund
        val correctHeight = !(selfOut.R5[Int].isDefined)
        val correctValue = selfOut.value >= SELF.value
        sigmaProp(selfPreserved && correctHeight && correctValue) && proveDlog(ownerKey)
      } else if (action == 4) {
        // complete refund
        val refundNotificationPeriod = 14400 // 20 days
        val correctHeight = (SELF.R5[Int].get + refundNotificationPeriod) <= HEIGHT
        val refundLimit = SELF.R6[Long].get
        val correctValue = SELF.value - selfOut.value <= refundLimit
        sigmaProp(selfPreserved && correctHeight && correctValue) && proveDlog(ownerKey) // todo: check is it ok to check no other conditions
      } else {
        sigmaProp(false)
      }
    }

}