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
      // provides gold price in nanoErg / kg
      val goldOracle = CONTEXT.dataInputs(0)
      sigmaProp(selfPreserved)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      sigmaProp(false)
    }



}