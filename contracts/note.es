{
    // Contract paths:

    // spend: full or with change

    // redeem

    // to create a note, ...

    val g: GroupElement = groupGenerator

    val history = SELF.R4[AvlTree].get

    val holder = SELF.R5[GroupElement].get
    val holderBytesHash = blake2b256(proveDlog(holder).propBytes)

    val selfOutput = OUTPUTS(0)

    val action = getVar[Byte](0).get

    val noteTokenId = SELF.tokens(0)._1
    val noteValue = SELF.tokens(0)._2

    if(action == 0) {
      // spending path
      val reserve = CONTEXT.dataInputs(0)
      val leafKey = reserve.id

      val noteValueBytes = longToByteArray(noteValue)
      val message = noteValueBytes ++ noteTokenId

      // c of signature in (c, s)
      val cBytes = getVar[Coll[Byte]](1).get
      val c = byteArrayToBigInt(cBytes)

      // s of signature in (c, s)
      val sBytes = getVar[Coll[Byte]](2).get
      val s = byteArrayToBigInt(sBytes)

      val U = g.exp(s).multiply(holder.exp(c)).getEncoded // as a byte array

      val properLength = (cBytes.size == 32) && (sBytes.size == 32)
      val properSignature = properLength && (cBytes == blake2b256(U ++ message))

      val properReserve = blake2b256(reserve.propositionBytes) == holderBytesHash

      val leafValue = cBytes ++ sBytes
      val keyVal = (leafKey, leafValue)
      val proof = getVar[Coll[Byte]](3).get

      val nextTree: Option[AvlTree] = history.insert(Coll(keyVal), proof)
      // This will fail if the operation failed or the proof is incorrect due to calling .get on the Option
      val outputDigest: Coll[Byte] = nextTree.get.digest

      val insertionPerformed = selfOutput.R4[AvlTree].get.digest == outputDigest
      val sameScript = selfOutput.propositionBytes == SELF.propositionBytes
      val nextHolderDefined = selfOutput.R5[AvlTree].isDefined

      val tokensPreserved = selfOutput.tokens(0) == SELF.tokens(0) // todo: spend with change

      sigmaProp(sameScript && insertionPerformed && properSignature && properReserve && nextHolderDefined && tokensPreserved)
    } else {
      // redeem path
      val reserve = CONTEXT.dataInputs(0)
      val reserveId = reserve.id
      val proof = getVar[Coll[Byte]](1).get
      val value = history.get(reserveId, proof).get

      val cBytes = value.slice(0, 32)
      val sBytes = value.slice(32, 64)
      val c = byteArrayToBigInt(cBytes)
      val s = byteArrayToBigInt(sBytes)

      val maxValueBytes = getVar[Coll[Byte]](2).get
      val message = maxValueBytes ++ noteTokenId

      val U = g.exp(s).multiply(holder.exp(c)).getEncoded // as a byte array

      val properSignature = cBytes == blake2b256(U ++ message)



      sigmaProp(properSignature)
    }

}