package fi.spectrum.sim

import sigmastate.Values.{ConstantNode, EvaluatedValue}

object sigma {
  def transformVal[V <: EvaluatedValue[_]](v: V): Any =
    v match {
      case ConstantNode(array: special.collection.CollOverArray[Any @unchecked], _) =>
        CollOpaque(array.toArray.toVector)
      case ConstantNode(p @ sigmastate.eval.CSigmaProp(_), _) => SigmaProp(p.propBytes.toArray.toVector)
      case ConstantNode(v, _)                                 => v
      case v                                                  => v
    }
}
