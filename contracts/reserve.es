{
    // ERG variant

    // Actions:
    //  - redeem note
    //  - top up
    //  - unlock refund, lock refund, refund

    val selfInput = INPUTS(0)
    val noteInput = INPUTS(1)

    val selfOut = OUTPUTS(0)

    val action = getVar[Byte](0).get

    if (action == 0) {
      // provides gold price in nanoErg / kg
      val goldOracle = CONTEXT.dataInputs(0)
      sigmaProp(true)
    } else {
      sigmaProp(true)
    }


}