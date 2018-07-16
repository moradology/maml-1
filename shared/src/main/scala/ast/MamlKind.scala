package com.azavea.maml.ast

import io.circe._
import cats._
import cats.implicits._

import scala.util.Try


sealed trait MamlKind { def repr: String }
object MamlKind {
  case object Bool extends MamlKind { def repr: String = "bool" }
  case object Int extends MamlKind { def repr: String = "int" }
  case object Double extends MamlKind { def repr: String = "double" }
  case object Tile extends MamlKind { def repr: String = "raster" }
  case object Geom extends MamlKind { def repr: String = "geom" }
  // useful for writing inductive proofs/handling certain classes of failure
  case object Void extends MamlKind { def repr: String = "void" }

  def fromString(str: String): Option[MamlKind] = Try {
    str match {
      case "bool" => MamlKind.Bool
      case "int" => MamlKind.Int
      case "double" => MamlKind.Double
      case "raster" => MamlKind.Tile
      case "geom" => MamlKind.Geom
      case "void" => MamlKind.Void
    }
  }.toOption

  implicit lazy val decodeMamlKind: Decoder[MamlKind] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(fromString(str).get).leftMap(t => "MamlKind")
  }

  implicit lazy val encodeMamlKind: Encoder[MamlKind] =
    Encoder.encodeString.contramap[MamlKind](_.repr)
}
