# ChainCash - elastic peer-to-peer money creation via trust and blockchain assets

This repository contains whitepaper, modelling code and prototype implementation for 
ChainCash, a protocol to create money in self-sovereign way via trust or collateral, with collective backing and 
individual acceptance. 

## Contents

* Whitepaper - https://github.com/kushti/chaincash/blob/master/paper/chaincash.pdf
High-level description of ChinCash protocol and its implementation

* Modelling - https://github.com/kushti/chaincash/tree/master/src/main/scala/chaincash/model
Contract-less and blockchain-less models of ChainCash entities and one of notes collateralization 
estimation options.

* Contracts - https://github.com/kushti/chaincash/tree/master/contracts - note and reserve contracts in ErgoScript
* Tests - https://github.com/kushti/chaincash/blob/master/src/test/scala/kiosk/NoteSpec.scala - Kiosk-based tests for transactions involving note
 contracts (note creation, spending, redemption)
* Offchain part - https://github.com/kushti/chaincash/tree/master/src/main/scala/chaincash/offchain - on-chain data tracking,
  persistence, transaction builders


## TODO

* modify note contract to have ability to spend multiple notes in a single transaction. 
For that, make action == -1 means redemption, otherwise, action means payment output index
* note acceptance predicates
* test payments with change (currently only full note amount being paid is tested only)
* support few spendings of a note in the same block
* support other tokens in reserves, e.g. SigUSD
* efficient persistence for own notes (currently, all the notes in the system are iterated over)

and then, API and some front-end on top of it