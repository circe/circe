package io.circe

import cats.laws.discipline.TraverseTests
import io.circe.JsonF.{ foldJson, unfoldJson }
import io.circe.tests.CirceSuite

class JsonFSuite extends CirceSuite {

  checkLaws("Traverse[JsonF]", TraverseTests[JsonF].traverse[Int, Int, Int, Set[Int], Option, Option])

  "fold then unfold" should "be identity " in forAll { jsonF: JsonF[Json] =>
    assert(unfoldJson(foldJson(jsonF)) === jsonF)
  }

  "unfold then fold" should "be identity " in forAll { json: Json =>
    assert(foldJson(unfoldJson(json)) === json)
  }

}
