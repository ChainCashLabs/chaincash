{
  val action = getVar[Byte](0).get
  if(action == 0) {

     val noteTokenId = SELF.tokens(0)._1
     val noteValue = SELF.tokens(0)._2

     val g: GroupElement = groupGenerator

     val holder = SELF.R5[GroupElement].get

     val noteValueBytes = longToByteArray(noteValue)
     val message = noteValueBytes ++ noteTokenId

     // Computing challenge
     val e: Coll[Byte] = blake2b256(message) // weak Fiat-Shamir
     val eInt = byteArrayToBigInt(e) // challenge as big integer

     // a of signature in (a, z)
     val a = getVar[GroupElement](1).get

     // z of signature in (a, z)
     val zBytes = getVar[Coll[Byte]](2).get
     val z = byteArrayToBigInt(zBytes)

     // Signature is valid if g^z = a * x^e
     val properSignature = g.exp(z) == a.multiply(holder.exp(eInt))

     sigmaProp(properSignature)
  } else {
     sigmaProp(false)
  }
}