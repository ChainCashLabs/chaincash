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
    // * wrong position (there is a leaf in the tree with the same position) - collateral seized - done
    // * wrong collateral - done in redemption request contract
    // * tree leaf not known (collateral not seized) - done
    // * earlier reserve exists (collateral not seized) - done
    // * tree cut - collateral seized - done
    // * double spend - collateral seized - to be done in reserve
    // * wrong value transition - collateral seized
    // * wrong leaf (signature) in the tree  - collateral seized
    // * wrong link in the tree - collateral seized
    // * wrond redeem reserve id @ redeem position - collateral seized

    val action = getVar[Byte](0).get

    // todo: split into action contracts like in dexy
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
          val currentPosition = SELF.R5[Long].get
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
      } else if (action == -3) {

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
      } else if (action == -4) {
        // tree cut - collateral seized
        // here, we check that there is a signature from current holder for a record after last tree's leaf
        // we have to check last leaf also, to check holder correctness
        val lastLeafReserve = CONTEXT.dataInputs(0)
        val holderReserve = CONTEXT.dataInputs(1)

        val g: GroupElement = groupGenerator

        // checking last leaf signature
        val lastLeafTreeHashDigest = getVar[Coll[Byte]](1).get
        val lastLeafReserveId = getVar[Coll[Byte]](2).get
        val lastLeafNoteValue = getVar[Long](3).get
        val lastLeafHolderId = getVar[Coll[Byte]](4).get
        val lastLeafA = getVar[GroupElement](5).get
        val lastLeafABytes = lastLeafA.getEncoded
        val lastLeafZBytes = getVar[Coll[Byte]](6).get
        val lastLeafProperFormat = lastLeafTreeHashDigest.size == 32 && lastLeafReserveId.size == 32 && lastLeafHolderId.size == 32
        val lastLeafMessage = lastLeafTreeHashDigest ++ lastLeafReserveId ++ longToByteArray(lastLeafNoteValue) ++ lastLeafHolderId
        val lastLeafEInt = byteArrayToBigInt(blake2b256(lastLeafMessage)) // weak Fiat-Shamir
        val lastLeafZ = byteArrayToBigInt(lastLeafZBytes)
        val lastLeafReserveIdValid = lastLeafReserve.tokens(0)._1 == lastLeafReserveId
        val lastLeafReservePk = lastLeafReserve.R4[GroupElement].get
        val lastLeafProperSignature = (g.exp(lastLeafZ) == lastLeafA.multiply(lastLeafReservePk.exp(lastLeafEInt))) && lastLeafProperFormat && lastLeafReserveIdValid

        // checking last leaf tree proof
        val lastLeafPosition = SELF.R5[Long].get
        val keyBytes = longToByteArray(lastLeafPosition)
        val proof = getVar[Coll[Byte]](7).get
        val history = SELF.R4[AvlTree].get
        val properProof = history.get(keyBytes, proof).get == (lastLeafABytes ++ lastLeafZBytes)

        // check holder's record signed
        val holderTreeHashDigest = getVar[Coll[Byte]](8).get
        val holderReserveId = getVar[Coll[Byte]](9).get
        val holderNoteValue = getVar[Long](10).get
        val holderHolderId = getVar[Coll[Byte]](11).get
        val holderA = getVar[GroupElement](12).get
        val holderABytes = holderA.getEncoded
        val holderZBytes = getVar[Coll[Byte]](13).get
        val holderProperFormat = holderTreeHashDigest.size == 32 && holderReserveId.size == 32 && holderHolderId.size == 32
        val holderMessage = holderTreeHashDigest ++ holderReserveId ++ longToByteArray(holderNoteValue) ++ holderHolderId
        val holderEInt = byteArrayToBigInt(blake2b256(holderMessage)) // weak Fiat-Shamir
        val holderZ = byteArrayToBigInt(holderZBytes)
        val holderReserveIdValid = holderReserve.tokens(0)._1 == holderReserveId
        val holderReservePk = holderReserve.R4[GroupElement].get
        val holderProperSignature = (g.exp(holderZ) == holderA.multiply(holderReservePk.exp(holderEInt))) && holderProperFormat && holderReserveIdValid

        // checking link between the last leaf and holder record
        val linkCorrect = SELF.R4[AvlTree].get.digest == holderTreeHashDigest && holderReserveId == lastLeafHolderId

        lastLeafProperSignature && properProof && holderProperSignature && linkCorrect
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
}