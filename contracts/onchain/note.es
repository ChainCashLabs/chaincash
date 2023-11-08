{
    // Note contract

    // It has two execution paths:

    // spend: full spending or with change

    // redeem:

    // when redemption, receipt is created which is allowing to do another redemption against earlier reserve

    // to create a note, ...

    // box data:
    // R4 - history of ownership (under AVL+ tree),
    //      tree contains reserveId as a key, signature as value,
    //      and message is note value and token id
    // R5 - current holder of the note (public key given as a group element)
    // R6 - current length of the chain (as long int)

    val g: GroupElement = groupGenerator

    val history = SELF.R4[AvlTree].get

    val action = getVar[Byte](0).get // also encodes note output # in tx outputs

    val noteTokenId = SELF.tokens(0)._1
    val noteValue = SELF.tokens(0)._2

    val reserve = CONTEXT.dataInputs(0)
    val reserveId = reserve.tokens(0)._1

    val holder = SELF.R5[GroupElement].get

    if (action >= 0) {
      // spending path

      val selfOutput = OUTPUTS(action)

      val position = SELF.R6[Long].get
      val positionBytes = longToByteArray(position)
      val noteValueBytes = longToByteArray(noteValue)
      val message = positionBytes ++ noteValueBytes ++ noteTokenId

      // Computing challenge
      val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
      val eInt = byteArrayToBigInt(e) // challenge as big integer

      // a of signature in (a, z)
      val a = getVar[GroupElement](1).get
      val aBytes = a.getEncoded

      // z of signature in (a, z)
      val zBytes = getVar[Coll[Byte]](2).get
      val z = byteArrayToBigInt(zBytes)

      // Signature is valid if g^z = a * x^e
      val properSignature = g.exp(z) == a.multiply(holder.exp(eInt))

      val properReserve = holder == reserve.R4[GroupElement].get

      val leafValue = aBytes ++ zBytes
      val keyVal = (reserveId, leafValue)
      val proof = getVar[Coll[Byte]](3).get

      val nextTree: Option[AvlTree] = history.insert(Coll(keyVal), proof)
      // This will fail if the operation failed or the proof is incorrect due to calling .get on the Option
      val outputDigest: Coll[Byte] = nextTree.get.digest

      def nextNoteCorrect(noteOut: Box): Boolean = {
          val insertionPerformed = noteOut.R4[AvlTree].get.digest == outputDigest
          val sameScript = noteOut.propositionBytes == SELF.propositionBytes
          val nextHolderDefined = noteOut.R5[GroupElement].isDefined
          val valuePreserved = noteOut.value >= SELF.value
          val positionIncreased = noteOut.R6[Long].get == (position + 1)

          positionIncreased && insertionPerformed && sameScript && nextHolderDefined && valuePreserved
      }

      val changeIdx = getVar[Byte](4) // optional index of change output

      val outputsValid = if(changeIdx.isDefined) {
        val changeOutput = OUTPUTS(changeIdx.get)

        // strict equality to prevent moving tokens to other contracts
        (selfOutput.tokens(0)._2 + changeOutput.tokens(0)._2) == SELF.tokens(0)._2 &&
            nextNoteCorrect(selfOutput) &&
            nextNoteCorrect(changeOutput)
      } else {
        selfOutput.tokens(0) == SELF.tokens(0) && nextNoteCorrect(selfOutput)
      }

      sigmaProp(properSignature && outputsValid)
    } else {
      // redemption path
      // called by setting action variable to any negative value, -1 considered as standard by offchain apps

      // we just check current holder's signature here

      // it is checked that note token is locked in receipt in the reserve contract

      // we check that the note is spent along with a reserve contract box.
      // for that, we fix reserve input position @ #1
      // we drop version byte during ergotrees comparison
      // signature of note holder is also required

      val reserveInputErgoTree = INPUTS(1).propositionBytes
      val treeHash = blake2b256(reserveInputErgoTree.slice(1, reserveInputErgoTree.size))
      val reserveSpent = treeHash == fromBase58("2DfY1K4rW9zPVaQgaDp2KXgnErjxKPbbKF5mq1851MJE")
      proveDlog(holder) && sigmaProp(reserveSpent)
    }

}