package fi.spectrum.sim

import cats.Eval
import fi.spectrum.sim.runtime.Ledger
import io.circe.derivation.deriveDecoder
import io.circe.{Decoder, Json, Printer}
import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction, JsonCodecs}
import scorex.util.encode.Base16
import sigmastate.interpreter.ContextExtension
import sttp.client3._
import sttp.client3.circe.asJson
import sttp.client3.okhttp.OkHttpSyncBackend
import sttp.model.MediaType

final case class RuntimeSetup[Spec[_[_]]](box: Spec[Ledger], ctx: RuntimeCtx) {
  def run(implicit ev: Spec[Ledger] <:< BoxSpec[Ledger]): Eval[Boolean] = ev(box).validator.run(ctx).map(_._2)
}

object RuntimeSetup extends JsonCodecs {

  def fromTx[Box[_[_]]](tx: ErgoLikeTransaction, selfInputIx: Int, height: Int)(implicit
    fromBox: TryFromBox[Box, Ledger]
  ): Option[RuntimeSetup[Box]] = {
    val (inputs, outputs) = pullIOs(tx)
    RuntimeSetup.fromIOs[Box](inputs, outputs, selfInputIx, height)
  }

  def fromIOs[Box[_[_]]](
    inputs: List[(ErgoBox, ContextExtension)],
    outputs: List[ErgoBox],
    selfInputIx: Int,
    height: Int
  )(implicit fromBox: TryFromBox[Box, Ledger]): Option[RuntimeSetup[Box]] = {
    val (selfIn, ext) = inputs(selfInputIx)
    for {
      selfBox <- fromBox.tryFromBox(selfIn)
      ctx = RuntimeCtx(
        height,
        vars = ext.values.toVector.map { case (ix, c) =>
          ix.toInt -> sigma.transformVal(c)
        }.toMap,
        inputs  = inputs.map(_._1).map(AnyBoxSpec.tryFromBox.tryFromBox).collect { case Some(x) => x },
        outputs = outputs.map(AnyBoxSpec.tryFromBox.tryFromBox).collect { case Some(x) => x }
      )
    } yield RuntimeSetup(selfBox, ctx)
  }

  private lazy val backend = OkHttpSyncBackend()

  private def pullIOs(tx: ErgoLikeTransaction): (List[(ErgoBox, ContextExtension)], List[ErgoBox]) = {
    val inputs = tx.inputs.flatMap(i => pullBox(i.boxId).map(_ -> i.spendingProof.extension))
    inputs.toList -> tx.outputs.toList
  }

  private def pullBox(id: BoxId): Option[ErgoBox] = basicRequest
    .post(uri"https://gql.ergoplatform.com/")
    .body(bodyJson(id))
    .response(asJson[Data])
    .send(backend)
    .body
    .right
    .toOption
    .flatMap(_.data.boxes.headOption)

  def body(id: BoxId): String =
    s"""{"query":"{boxes(boxId:\\"${Base16.encode(
      id
    )}\\") {boxId,value,creationHeight,transactionId,index,ergoTree,additionalRegisters,assets{tokenId,amount,}}}"}"""
  def bodyJson(id: BoxId): Json = io.circe.parser.parse(body(id)).toOption.get

  // curl 'https://gql.ergoplatform.com/' -H 'Accept-Encoding: gzip, deflate, br' -H 'Content-Type: application/json' -H 'Accept: application/json' -H 'Connection: keep-alive' -H 'DNT: 1' -H 'Origin: https://gql.ergoplatform.com' --data-binary '{"query":"{\n  boxes(boxId: \"e8cb8e8acbebab6b9ba3706904facd70393f5731c7e36a55caa660fde5419b60\") {\n    boxId,\n    value,\n    creationHeight,\n    index,\n    ergoTree,\n    additionalRegisters,\n    assets {\n      tokenId,\n      amount,\n    }\n  }\n}"}' --compressed

  implicit val jsonSerializer: BodySerializer[Json] = json =>
    StringBody(json.printWith(Printer.noSpaces), "utf-8", MediaType.ApplicationJson)

  case class Boxes(boxes: List[ErgoBox])
  object Boxes {
    implicit val decoder: Decoder[Boxes] = deriveDecoder
  }

  case class Data(data: Boxes)
  object Data {
    implicit val decoder: Decoder[Data] = deriveDecoder
  }
}
