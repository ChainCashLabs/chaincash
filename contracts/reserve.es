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
      // provides gold price in nanoErg per kg
      val goldOracle = CONTEXT.dataInputs(0)
      val oracleRate = goldOracle.R4[Long] / 1000000 // normalize to nanoerg per mg of gold
      val tokensRedeemed = INPUTS(0).tokens(0)._2 // 1 token == 1 mg of gold
      val nanoergsToRedeem = tokensRedeemed * oracleRate
      val redeemCorrect = (SELF.value - selfOut.value) <= nanoergsToRedeem
      sigmaProp(selfPreserved && redeemCorrect)
    } else if (action == 1) {
      // top up
      sigmaProp(selfPreserved && (selfOut.value - SELF.value >= 1000000000)) // at least 1 ERG added
    } else {
      // todo: implement refund
      sigmaProp(false)
    }



}