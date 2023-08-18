# ChainCash - elastic peer-to-peer money creation via trust and blockchain assets

This repository contains whitepaper, modelling code and prototype implementation for 
ChainCash, a protocol to create money in self-sovereign way via trust or collateral, with collective backing and 
individual acceptance. 

## Intro 

We consider money as a set of digital notes, and every note is collectively backed 
by all the previous spenders of the note. Every agent may create reserves to be used 
as collateral. When an agent spends note, whether received previously from another 
agent or just created by the agent itself, it is attaching its signature to it. 
A note could be redeemed at any time against any of reserves of agents previously 
signed the note. We allow an agent
to issue and spend notes without a reserve. It is up to agent's counter-parties
then whether to accept and so back an issued note with collateral or agent's
trust or not.

As an example, consider a small gold mining cooperative in Ghana issuing a
note backed by (tokenized) gold. The note is then accepted by the national government as mean of tax payment. Then the government is using the note (which
is now backed by gold and also trust in Ghana government, so, e.g. convertible
to Ghanaian Cedi as well) to buy oil from a Saudi oil company. Then the oil
company, having its own oil reserve also, is using the note to buy equipment
from China. Now a Chinese company has a note which is backed by gold, oil,
and Cedis.

Economic agent's individual note quality estimation predicate Pi(n) is considering collaterals
and trust of previous spenders. Different agents may have different collateralization 
estimation algorithm (by analyzing history of the single note n, or
all the notes known), different whitelists, blacklists, or trust scores assigned to
previous spenders of the note n etc. So in general case payment sender first
need to consult with the receiver on whether the payment (consisting of one or
multiple notes) can be accepted. However, in the real world likely there will be
standard predicates, thus payment receiver (e.g. an online shop) may publish
its predicate (or just predicate id) online, and then the payment can be done
without prior interaction.

## Whitepaper And Other Materials

High-level description of ChainCash protocol and its implementation can be found 
in the [whitepaper](https://github.com/kushti/chaincash/blob/master/paper/chaincash.pdf). 

More materials: 

* [The World Needs For More Collateral](https://www.ergoforum.org/t/the-world-needs-for-more-collateral/4451) - forum thread
* [Video presentation from Ergo Summit](https://www.youtube.com/watch?v=NxIlIpO6ZVI)

## Contents

* Whitepaper - https://github.com/kushti/chaincash/blob/master/paper/chaincash.pdf
High-level description of ChainCash protocol and its implementation

* Modelling - https://github.com/kushti/chaincash/tree/master/src/main/scala/chaincash/model
Contract-less and blockchain-less models of ChainCash entities and one of notes collateralization 
estimation options.

* Contracts - https://github.com/kushti/chaincash/tree/master/contracts - note and reserve contracts in ErgoScript
* Tests - https://github.com/kushti/chaincash/blob/master/src/test/scala/kiosk/ChainCashSpec.scala - Kiosk-based tests for transactions involving note
 contracts (note creation, spending, redemption)
* Offchain part - https://github.com/kushti/chaincash/tree/master/src/main/scala/chaincash/offchain - on-chain data tracking,
  persistence, transaction builders


## TODO

* refund with delay for reserves
* update ReserveData.liabilities and reserveKeys in offchain code
* offchain code for note redemption
* support few spendings of a note in the same block (offchain tracking of it)
* support other tokens in reserves, e.g. SigUSD
* efficient persistence for own notes (currently, all the notes in the system are iterated over)
* check ERG preservation in note contracts

and then, API for clients and some front-end on top of it