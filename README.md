# MAML: a Map Algebra Modeling Language

MAML is an attempt to produce an AST sufficient to describe common
operations on raster data. It includes serialization for this AST (via
the wonderful `Circe` library) and a reference implementation for
interpreting these ASTs as programs that yield imagery.


#### Extending the MAML AST

One of the key reasons for this library is to allow other libraries to
quickly and easily construct different Map Algebra programs which can be
interpreted in various contexts (e.g. single machine vs cluster; specified
in javascript and run on the JVM). In addition to providing different
interpreters for different computational contexts, it is sometimes desirable
to extend the implementation we've provided.
[This commit](https://github.com/geotrellis/maml/commit/ffcf3fa0db6a58b44aebfa30e0a099bfed590e43)
encapsulates all the work necessary to add a new node type to the AST,
provide it with the necessary serialization/deserialization logic and to
construct (and register with an `Interpreter`) the necessary `Directive`
used to evaluate it.

#### Changing Behavior of the Provided Interpreter

Because the structure of the `Interpreter`s we've provided is designed
to to be highly modular, individual directives can be replaced
willy-nilly. There are two necessary steps
1. construction of the `Directive`
2. registration of the `Directive` when constructing an `Interpreter`
   instance

###### Constructing a Directive

First, note that `Directive`s are really just `PartialFunction`s from an
`Expression` and its (evaluated) children to some evaluated result
(which is of type `Result` and can contain Imagery as well as numeric
and geometric results). Here's an example of defining the behavior of
our interpreter whenever it runs into an `Addition` node that's supposed
to produce imagery:

```scala
val myAddition = Directive { case (a@Addition(_), childResults) if (a.kind == MamlKind.Image) =>
  // Children results grouped according to their reported MamlKind
  val grouped = childResults.groupBy(_.kind)

  // Helpers that unwrap the contents of Result instances
  def doubleResults(grouped: Map[MamlKind, Seq[Result]]): Interpreted[List[Double]] =
    grouped.getOrElse(MamlKind.Double, List.empty).map(_.as[Double]).toList.sequence
  def intResults(grouped: Map[MamlKind, Seq[Result]]): Interpreted[List[Int]] =
    grouped.getOrElse(MamlKind.Int, List.empty).map(_.as[Int]).toList.sequence
  def imageResults(grouped: Map[MamlKind, Seq[Result]]): Interpreted[List[LazyMultibandRaster]] =
    grouped(MamlKind.Image).map(_.as[LazyMultibandRaster]).toList.sequence

  // Carry out addition on scalars first for optimal performance (commutativity ftw)
  val scalarSums =
    (doubleResults(grouped), intResults(grouped)).mapN { case (dbls, ints) => dbls.sum + ints.sum }

  // Finally, reduce over the children images, adding them together and finally supplementing each
  //  resultant cell's value with the scalarSums from above
  (imageResults(grouped), scalarSums).mapN { case (tiles, sums) =>
    val tileSum = tiles.reduce({ (lt1: LazyMultibandRaster, lt2: LazyMultibandRaster) => lt1.dualCombine(lt2, {_ + _}, {_ + _}) })
    ImageResult(tileSum.dualMap({ i: Int => i + sums.toInt }, { i: Double => i + sums }))
  }
}
```

Because the construct underlying `Directive` is simply that of the
partial function, we are free to do whatever we like in the body of the
function.


###### Constructing a new Interpreter

`Interpreter`s are little more than a set of `Directive`s, an ordering
for their attempted application (earlier listed directives are attempted
sooner), and a fallback directive (provided by the `Interpreter` and used
whenever an unrecognized node is encountered). Here's an example capable
of handling `Addition` (using the above `Directive`) and nothing else:

```scala
NaiveInterpreter(List(myAddition))
```

A more complete example can be found
[here](https://github.com/geotrellis/maml/blob/ffcf3fa0db6a58b44aebfa30e0a099bfed590e43/jvm/src/main/scala/eval/NaiveInterpreter.scala#L37-L94)



