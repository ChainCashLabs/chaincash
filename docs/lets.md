How to implement LETS on ChainCash
----------------------------------

This article describes how Local Exchange Trading System (LETS) on top of ChainCash by using ChainCash Server. 

LETS
----

A local exchange trading system (LETS) is a local mutual credit association in which members are allowed 
to create common credit money individually, written into a common ledger.

As an example, assume that Alice, with zero balance, is willing to buy a litre of raw milk from Bob.

First, they agree on a price; for example, assume that the price is about 2 Euro (as Alice and Bob are living 
in Ireland). After the deal is written into a ledger, Alice's balance becomes -2 (minus two) Euro, and Bob's balance 
becomes 2 Euro. Then Bob may spend his 2 Euro, for example, on homemade beer from Charlie. Such systems often impose 
limits on negative balances, and sometimes even on positive ones, to promote exchange in the community.

LETS on ChainCash
-----------------

* LETS members are decided off-chain
* LETS members white-list each other in ChainCash Server (todo: what kind of whitelist?)
* standard CCIP-1 contracts are used
* there is no requirement for min reserve value, so it can be close to zero ERG (slightly above to cover 
  storage rent requirements, eg 0.001 ERG)
* then for Alice to pay to Bob in our example, she is issuing a note with needed amount and pays Bob with it. Then 
  Bob may use it to pay Charlie. Bob may use multiple notes in one transaction. 