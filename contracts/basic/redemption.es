{

      val action = getVar[Byte](0).get

      if (action > 0) { // action == # of note input
        // init
        val selfOutput = OUTPUTS(0)
        val noteInput = INPUTS(action)


      } else if (action == -1) {
         // dispute
      } else {
         // redemption
      }

      // there are three redemption related actions:
      //  * if Alice has enough reserves , Bob may redeem from it (get ERGs worth of the note)
      //  * if Alice does not have reserves, but Charlie signed next after Alice, the obligation to redeem the note can
      //    be transferred to Charlie
      //  * if Bob is trying to redeem from Alice, but then there was Charlie to whom the note was spent after,
      //    the right to obtain reserve can be moved to Charlie
      // no partial redemptions are supported

      // we just check current holder's signature here
      //todo: check that note token burnt ? or could be done offchain only?
      //todo: check that another box with the same tree and tokens could not be spent

      val history = SELF.R4[AvlTree].get

      val zeroPosBytes = longToByteArray(0)
      val reserveId = getVar[Coll[Byte]](1).get
      val key = zeroPosBytes ++ reserveId

      val deadline = SELF.R5[Int].get

      val proof = getVar[Coll[Byte]](2).get
      if (history.get(key, proof).isDefined) {
        val deadlineMet = HEIGHT <= deadline
        sigmaProp(deadlineMet)
      } else {
        false
      }
}