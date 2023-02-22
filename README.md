# ChainCash - elastic peer-to-peer money creation via trust and blockchain assets

This repository contains whitepaper, modelling code and prototype implementation for 
ChainCash. 

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


## TODO: 

* modify note contract to have ability to spend multiple notes in a single transaction. 
For that, make action == -1 means redemption, otherwise, action means payment output index