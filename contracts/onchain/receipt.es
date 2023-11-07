{
    // receipt contract
    // it is possible to spend this box 3 years after, with tokens being necessarily burnt

    // registers:
    // R4 - AvlTree - history of ownership for corresponding redeemed note
    // R5 - Long - redeemed position
    // R6 - approx. height when this box was created

    def noTokens(b: Box) = b.tokens.size == 0
    val noTokensInOutputs = OUTPUTS.forall(noTokens)

    val creationHeight = SELF.R6[Int].get
    val burnPeriod = 788400 // 3 years

    val burnDone = (HEIGHT > creationHeight + burnPeriod) && noTokensInOutputs

    burnDone // todo: any other conditions ?
}