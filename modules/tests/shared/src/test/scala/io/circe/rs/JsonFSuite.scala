package io.circe.rs

import cats.instances.option._
import cats.kernel.instances.set._
import cats.kernel.instances.int._
import cats.laws.discipline.TraverseTests
import cats.syntax.eq._
import io.circe.Json
import io.circe.rs.JsonF.{ foldJson, unfoldJson }
import io.circe.tests.CirceSuite

class JsonFSuite extends CirceSuite {
  checkAll("Traverse[JsonF]", TraverseTests[JsonF].traverse[Int, Int, Int, Set[Int], Option, Option])

  "fold then unfold" should "be identity " in forAll { (jsonF: JsonF[Json]) =>
    assert(unfoldJson(foldJson(jsonF)) === jsonF)
  }

  "unfold then fold" should "be identity " in forAll { (json: Json) =>
    assert(foldJson(unfoldJson(json)) === json)
  }
}
