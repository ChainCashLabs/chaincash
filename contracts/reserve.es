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
      // provides gold price in nanoErg / kg
      val goldOracle = CONTEXT.dataInputs(0)
      sigmaProp(selfPreserved)
    } else {
      sigmaProp(selfPreserved)
    }


}