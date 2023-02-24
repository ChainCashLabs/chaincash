{
    // Note contract

    // It has two execution paths:

    // spend: full or with change

    // redeem:

    // to create a note, ...

    // box data:
    // R4 - history of ownership (under AVL+ tree)
    // R5 - current holder of the note (public key given as a group element)

    val g: GroupElement = groupGenerator

    val history = SELF.R4[AvlTree].get

    val action = getVar[Byte](0).get

    val noteTokenId = SELF.tokens(0)._1
    val noteValue = SELF.tokens(0)._2

    val reserve = CONTEXT.dataInputs(0)
    val reserveId = reserve.tokens(0)._1

    val holder = SELF.R5[GroupElement].get

    if(action >= 0) {
      // spending path

      val selfOutput = OUTPUTS(action)

      val noteValueBytes = longToByteArray(noteValue)
      val message = noteValueBytes ++ noteTokenId

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

      val insertionPerformed = selfOutput.R4[AvlTree].get.digest == outputDigest
      val sameScript = selfOutput.propositionBytes == SELF.propositionBytes
      val nextHolderDefined = selfOutput.R5[GroupElement].isDefined

      val changeIdx = getVar[Byte](4)
      val tokensPreserved = if(changeIdx.isDefined) {
        val changeOutput = OUTPUTS(changeIdx.get)

        (selfOutput.tokens(0)._2 + changeOutput.tokens(0)._2) == SELF.tokens(0)._2 &&
            changeOutput.tokens(0)._1 == noteTokenId &&
            selfOutput.tokens(0)._1 == noteTokenId &&
            changeOutput.propositionBytes == SELF.propositionBytes
      } else {
        selfOutput.tokens(0) == SELF.tokens(0)
      }

      val valuePreserved = selfOutput.value >= SELF.value

      sigmaProp(sameScript && insertionPerformed && properSignature && properReserve && nextHolderDefined && tokensPreserved)
    } else {
      // redemption path

      // todo: action == -1 to redeem, otherwise, action = output # ?

      // we just check current holder's signature here

      //todo: check that note token burnt
      //todo: check that another box with the same tree and tokens could not be spent

      proveDlog(holder)
    }

}