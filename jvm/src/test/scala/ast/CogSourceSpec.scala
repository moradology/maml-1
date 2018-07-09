package com.azavea.maml.eval

import com.azavea.maml.ast._
import com.azavea.maml.dsl._
import com.azavea.maml.eval._
import com.azavea.maml.eval.tile._
import com.azavea.maml.eval.directive.SourceDirectives._
import com.azavea.maml.eval.directive.OpDirectives._
import com.azavea.maml.ast.codec.tree.ExpressionTreeCodec

import io.circe._
import io.circe.syntax._
import geotrellis.raster._
import geotrellis.vector._
import cats._
import cats.data.{NonEmptyList => NEL, _}
import Validated._
import org.scalatest._

import scala.reflect._
import java.net.URI


class CogSourceSpec extends FunSpec with Matchers {
  import com.azavea.maml.ast.codec.tree.ExpressionTreeCodec
  import com.azavea.maml.ast.codec._
  val parser = new ExpressionTreeCodec
  parser.addRules(
    PartialFunction[Json, Decoder.Result[Expression]] { case expr if expr._type == Some("CogSource") => expr.as[CogSource] },
    PartialFunction[Expression, Json] { case cs@CogSource(_, _, _) => cs.asJson }
  )

  it("Should handle serde for custom cog source") {
    val uri = new URI("http://google.com")
    val ast = Subtraction(List(CogSource(uri, 1, None), IntLiteral(5)))
    println("TEST1", parser.encode(ast))
    val serialized = parser(ast)
    println("TEST2", parser.decode(serialized))
    parser(serialized)
    val deserialized = parser.decode(serialized)
  }
}
