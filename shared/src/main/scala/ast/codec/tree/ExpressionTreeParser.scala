package com.azavea.maml.ast.codec.tree

import com.azavea.maml.ast._
import com.azavea.maml.ast.codec._

import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._
import io.circe.generic.extras.Configuration

import java.security.InvalidParameterException
import scala.collection.mutable


class ExpressionTreeCodec extends MamlOperationCodecs
    with MamlSourceCodecs
    with MamlUtilityCodecs
    with Encoder[Expression]
    with Decoder[Expression] {
  type DecodeRule = PartialFunction[Json, Result[Expression]]
  type EncodeRule = PartialFunction[Expression, Json]

  implicit def conf: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")

  /** DECODING */
  val decodeRules: mutable.ArrayBuffer[DecodeRule] = mutable.ArrayBuffer(
    { case expr if expr._type == Some("IntLiteral") => expr.as[IntLiteral] },
    { case expr if expr._type == Some("DoubleLiteral") => expr.as[DoubleLiteral] },
    { case expr if expr._type == Some("BoolLiteral") => expr.as[BoolLiteral] },
    { case expr if expr._type == Some("Addition") => expr.as[Addition] },
    { case expr if expr._type == Some("Subtraction") => expr.as[Subtraction] },
    { case expr if expr._type == Some("Multiplication") => expr.as[Multiplication] },
    { case expr if expr._type == Some("Division") => expr.as[Division] },
    { case expr if expr._type == Some("Max") => expr.as[Max] },
    { case expr if expr._type == Some("Min") => expr.as[Min] },
    { case expr if expr._type == Some("Masking") => expr.as[Masking] },
    { case expr if expr._type == Some("Classification") => expr.as[Classification] },
    { case expr if expr._type == Some("FocalMax") => expr.as[FocalMax] },
    { case expr if expr._type == Some("FocalMin") => expr.as[FocalMin] },
    { case expr if expr._type == Some("FocalMean") => expr.as[FocalMean] },
    { case expr if expr._type == Some("FocalMedian") => expr.as[FocalMedian] },
    { case expr if expr._type == Some("FocalMode") => expr.as[FocalMode] },
    { case expr if expr._type == Some("FocalSum") => expr.as[FocalSum] },
    { case expr if expr._type == Some("FocalStdDev") => expr.as[FocalStdDev] }
  )

  final val decodeFallback = PartialFunction[Json, Result[Expression]] { case expr =>
    Left(DecodingFailure(s"Unrecognized node: $expr", List(CursorOp.DownField("_type"))))
  }

  def totalDecodeRule(json: Json) = {
    println(s"DECODING WITH ${decodeRules.size} rules")
    decodeRules.reduceLeft(_ orElse _).orElse(decodeFallback)(json)
  }

  /** ENCODING */
  val encodeRules: mutable.ArrayBuffer[EncodeRule] = mutable.ArrayBuffer(
    { case il@IntLiteral(_) => il.asJson },
    { case node: DoubleLiteral => node.asJson },
    { case node: BoolLiteral => node.asJson },
    { case node: Addition => node.asJson },
    { case sub@Subtraction(_) => sub.asJson },
    { case node: Multiplication => node.asJson },
    { case node: Division => node.asJson },
    { case node: Max =>node.asJson },
    { case node: Min => node.asJson },
    { case node: Masking => node.asJson },
    { case node: Classification => node.asJson },
    { case node: FocalMax => node.asJson },
    { case node: FocalMin => node.asJson },
    { case node: FocalMean => node.asJson },
    { case node: FocalMedian => node.asJson },
    { case node: FocalMode => node.asJson },
    { case node: FocalSum => node.asJson },
    { case node: FocalStdDev => node.asJson }
  )

  final val encodeFallback = PartialFunction[Expression, Json] { case expr =>
    throw new InvalidParameterException(s"Unrecognized AST: $expr")
  }

  def totalEncodeRule(expr: Expression) = {
    println(s"ENCODING WITH ${encodeRules.size} rules")
    encodeRules.reduceLeft(_ orElse _).orElse(encodeFallback)(expr)
  }
  implicit def mamlDecoder: io.circe.Decoder[com.azavea.maml.ast.Expression] =
    Decoder.instance[Expression]({ hcurs => totalDecodeRule(hcurs.value) })

  implicit def mamlEncoder: io.circe.Encoder[com.azavea.maml.ast.Expression] =
    Encoder.encodeJson.contramap[Expression](totalEncodeRule)

  def addRules(decode: DecodeRule, encode: EncodeRule): Unit = {
    println("rules before", decodeRules.size, encodeRules.size)
    decode +=: decodeRules
    encode +=: encodeRules
    println("rules after", decodeRules.size, encodeRules.size)
  }

  // Decode
  def apply(c: HCursor): Result[Expression] = totalDecodeRule(c.value)

  def apply(json: Json): Result[Expression] = totalDecodeRule(json)

  def decode(json: Json): Result[Expression] = totalDecodeRule(json)

  // Encode
  def apply(expr: Expression): Json = totalEncodeRule(expr)

  def encode(expr: Expression): Json = totalEncodeRule(expr)
}

