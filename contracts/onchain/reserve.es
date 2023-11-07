{
    // ERG variant

    // Data:
    //  - token #0 - identifying singleton token
    //  - R4 - signing key (as a group element)
    //  - R5 - refund init height (Int.MaxValue if not set)
    //
    // Actions:
    //  - redeem note (#0)
    //  - top up      (#1)
    //  - init refund (#2)
    //  - cancel refund (#3)
    //  - complete refund (#4)

    val ownerKey = SELF.R4[GroupElement].get // used in notes and unlock/lock/refund actions
    val selfOut = OUTPUTS(0)
    val selfPreserved =
            selfOut.propositionBytes == SELF.propositionBytes &&
            selfOut.tokens == SELF.tokens &&
            selfOut.R4[GroupElement].get == SELF.R4[GroupElement].get

    val action = getVar[Byte](0).get

    if (action == 0) {
      // redeem path
      // oracle provides gold price in nanoErg per kg in its R4 register

      val g: GroupElement = groupGenerator

      val noteInput = INPUTS(0)
      val noteTokenId = noteInput.tokens(0)._1
      val noteValue = noteInput.tokens(0)._2 // 1 token == 1 mg of gold
      val history = noteInput.R4[AvlTree].get
      val reserveId = SELF.tokens(0)._1

      val goldOracle = CONTEXT.dataInputs(0)
      val properOracle = goldOracle.tokens(0)._1 == fromBase58("2DfY1K4rW9zPVaQgaDp2KXgnErjxKPbbKF5mq1851MJE")
      val oracleRate = goldOracle.R4[Long].get / 1000000 // normalize to nanoerg per mg of gold

      // 2% redemption fee
      val nanoergsToRedeem = noteValue * oracleRate * 98 / 100
      val redeemCorrect = (SELF.value - selfOut.value) <= nanoergsToRedeem

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

      // todo: check receipt output contract
      val receiptOut = OUTPUTS(1)
      val properReceipt =
        receiptOut.tokens(0) == noteInput.tokens(0) &&
        receiptOut.R4[AvlTree].get == history  &&
        receiptOut.R5[Long].get == position    &&
        receiptOut.R6[Int].get >= HEIGHT - 20  &&  // 20 blocks for inclusion
        receiptOut.R6[Int].get <= HEIGHT

      sigmaProp(selfPreserved && properOracle && redeemCorrect && properSignature && properReceipt)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      // todo: write tests for refund paths, document them
      if (action == 2) {
        // init refund
        val correctHeight = selfOut.R5[Int].get >= HEIGHT - 5
        sigmaProp(selfPreserved && correctHeight) && proveDlog(ownerKey)
      } else if (action == 3) {
        // cancel refund
        val correctHeight = !(selfOut.R5[Int].isDefined)
        sigmaProp(selfPreserved && correctHeight) && proveDlog(ownerKey)
      } else if (action == 4) {
        // complete refund
        val refundNotificationPeriod = 7200 // 10 days
        val correctHeight = (SELF.R5[Int].get + refundNotificationPeriod) >= HEIGHT
        sigmaProp(correctHeight) && proveDlog(ownerKey) // todo: check is it ok to check no conditions
      } else {
        sigmaProp(false)
      }
    }



}