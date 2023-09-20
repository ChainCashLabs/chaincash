{
    // redemption box contract
    //
    // Tokens:
    // #0 - redemption contract token
    //
    // Registers:
    // R4: history tree (position -> (a, z)), where (a, z) is sig for (prev tree hash, reserve id, note value, holder id)
    // R5: max position in the tree
    // R6: (redeem position, redeem reserve id)
    // R7: (max contested position, contestMode) - initially (-1, false)
    // R8: deadline

    // 720 blocks for contestation

    // Dispute actions:
    // * wrong position (there is a leaf in the tree with the same position) - collateral seized
    // * wrong collateral - collateral seized
    // * tree leaf not known (collateral not seized)
    // * earlier reserve exists (collateral not seized)
    // * tree cut - collateral seized
    // * double spend - collateral seized - to be done in reserve
    // * wrong value transition - collateral seized
    // * wrong leaf - collateral seized
    // * wrond redeem reserve id @ redeem position - collateral seized

    val action = getVar[Byte](0).get

    // todo: split into action contracts like dexy?
    val r: Boolean = if (action < 0) {
      // dispute
      if (action == -1) {
        // wrong max position (R5 register)
        // we check that there is leaf in the tree with current position exists

        val pos = SELF.R5[Long].get + 1

        val treeHashDigest = getVar[Coll[Byte]](1).get
        val reserveId = getVar[Coll[Byte]](2).get
        val noteValue = getVar[Long](3).get
        val holderId = getVar[Coll[Byte]](4).get
        val a = getVar[GroupElement](5).get
        val aBytes = a.getEncoded
        val zBytes = getVar[Coll[Byte]](7).get
        val properFormat = treeHashDigest.size == 32 && reserveId.size == 32 && holderId.size == 32
        val message = treeHashDigest ++ reserveId ++ longToByteArray(noteValue) ++ holderId

        // Computing challenge
        val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
        val eInt = byteArrayToBigInt(e) // challenge as big integer

        val g: GroupElement = groupGenerator
        val z = byteArrayToBigInt(zBytes)
        val reserve = CONTEXT.dataInputs(0)
        val reserveIdValid = reserve.tokens(0)._1 == reserveId
        val reservePk = reserve.R4[GroupElement].get
        val properSignature = (g.exp(z) == a.multiply(reservePk.exp(eInt))) && properFormat && reserveIdValid

        val proof = getVar[Coll[Byte]](8).get
        val history = SELF.R4[AvlTree].get
        val keyBytes = longToByteArray(pos)
        val properProof = history.get(keyBytes, proof).get == (aBytes ++ zBytes)

        // preservation not checked so collateral could be fully spent
        properSignature && properProof
      } else if (action == -2) {
        // wrong collateral
        SELF.value != 2000000000 // 2 ERG, make collateral configurable via data-input
      } else if (action == -3) {
        // tree leaf contents is asked or provided

        val selfOutput = OUTPUTS(0)

        val selfPreservationExceptR7 = selfOutput.tokens == SELF.tokens &&
                                       selfOutput.value == SELF.value &&
                                       selfOutput.R4[AvlTree].get == SELF.R4[AvlTree].get &&
                                       selfOutput.R5[Long].get == SELF.R5[Long].get &&
                                       selfOutput.R6[(Long, Coll[Byte])].get == SELF.R6[(Long, Coll[Byte])].get &&
                                       selfOutput.R7[(Long, Boolean)].get == SELF.R7[(Long, Boolean)].get &&
                                       selfOutput.R8[Int].get == SELF.R8[Int].get

        val r7 = SELF.R7[(Long, Boolean)].get
        val maxContestedPosition = r7._1
        val contested = r7._2
        if (contested) {
          // tree leaf provided
          val currentContestedPosition = maxContestedPosition + 1
          val reserve = CONTEXT.dataInputs(0)

          val treeHashDigest = getVar[Coll[Byte]](1).get
          val reserveId = getVar[Coll[Byte]](2).get
          val noteValue = getVar[Long](3).get
          val holderId = getVar[Coll[Byte]](4).get
          val a = getVar[GroupElement](5).get
          val aBytes = a.getEncoded
          val zBytes = getVar[Coll[Byte]](6).get
          val properFormat = treeHashDigest.size == 32 && reserveId.size == 32 && holderId.size == 32
          val message = treeHashDigest ++ reserveId ++ longToByteArray(noteValue) ++ holderId

          // Computing challenge
          val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
          val eInt = byteArrayToBigInt(e) // challenge as big integer

          val g: GroupElement = groupGenerator
          val z = byteArrayToBigInt(zBytes)
          val reserveIdValid = reserve.tokens(0)._1 == reserveId
          val reservePk = reserve.R4[GroupElement].get
          val properSignature = (g.exp(z) == a.multiply(reservePk.exp(eInt))) && properFormat && reserveIdValid

          val keyBytes = longToByteArray(currentContestedPosition)

          val proof = getVar[Coll[Byte]](7).get
          val currentPosition = SELF.R5[(Long, (Coll[Byte], Coll[Byte]))].get._1
          val history = SELF.R4[AvlTree].get
          val properProof = history.get(keyBytes, proof).get == (aBytes ++ zBytes)

          val outR7 = selfOutput.R7[(Long, Boolean)].get
          val outPositionCorrect = currentContestedPosition
          val outR7Valid = outR7._1 == outPositionCorrect && outR7._2 == false

          properProof && properSignature && selfPreservationExceptR7 && outR7Valid
        } else {
          // tree leaf asked
          val outR7 = selfOutput.R7[(Long, Boolean)].get
          val outR7Valid = outR7._1 == maxContestedPosition && outR7._2 == true
          // todo: move deadline
          if (maxContestedPosition == SELF.R5[Long].get) {
            // can't ask for more leafs when the whole tree walked through
            false
          } else {
            selfPreservationExceptR7 && outR7Valid
          }
        }
      } else if (action == -4) {

        // earlier reserve exists (collateral not seized)

        val selfOutput = OUTPUTS(0)
        val r6 = SELF.R6[(Long, Coll[Byte])].get
        val redeemPosition = r6._1
        val redeemReserveId = r6._2
        val alternativePosition = getVar[Long](1).get
        val redeemReserve = CONTEXT.dataInputs(0)
        val altReserve = CONTEXT.dataInputs(1)

        val selfPreservation = selfOutput.tokens == SELF.tokens &&
                               selfOutput.value == SELF.value &&
                               selfOutput.R4[AvlTree].get == SELF.R4[AvlTree].get &&
                               selfOutput.R5[Long].get == SELF.R5[Long].get &&
                               selfOutput.R6[(Long, Coll[Byte])].get == SELF.R6[(Long, Coll[Byte])].get &&
                               selfOutput.R7[(Long, Boolean)].get == SELF.R7[(Long, Boolean)].get &&
                               selfOutput.R8[Int].get == SELF.R8[Int].get

        // todo: implement alternative position and reserve id check
        altReserve.value >= redeemReserve.value && alternativePosition < redeemPosition && selfPreservation
      } else if (action == -5) {
        // tree cut - collateral seized
        // here, we check that there is a signature from current holder for a record after last tree's leaf
        // we have to check last leaf also, to check holder correctness
        val lastLeafReserve = CONTEXT.dataInputs(0)
        val holderReserve = CONTEXT.dataInputs(1)


        // todo: impl
        false
      } else if (action == -6) {
        // double spend
        false
      } else {
        // no more actions supported
        false
      }
    } else {
      // redemption
      // todo: implement
      false
    }

    sigmaProp(r)


      // there are three redemption related actions:
      //  * if Alice has enough reserves , Bob may redeem from it (get ERGs worth of the note)
      //  * if Alice does not have reserves, but Charlie signed next after Alice, the obligation to redeem the note can
      //    be transferred to Charlie
      //  * if Bob is trying to redeem from Alice, but then there was Charlie to whom the note was spent after,
      //    the right to obtain reserve can be moved to Charlie
      // no partial redemptions are supported

      // we just check current holder's signature here
      //todo: check that note token burnt ? or could be done offchain only?
      //todo: check that another box with the same tree and tokens could not be spent
/*
      val history = SELF.R4[AvlTree].get

      val zeroPosBytes = longToByteArray(0)
      val reserveId = getVar[Coll[Byte]](1).get
      val key = zeroPosBytes ++ reserveId

      val deadline = SELF.R5[Int].get

      val proof = getVar[Coll[Byte]](2).get
      if (history.get(key, proof).isDefined) {
        val deadlineMet = HEIGHT <= deadline
        sigmaProp(deadlineMet)
      } else {
        false
      }

      */

}