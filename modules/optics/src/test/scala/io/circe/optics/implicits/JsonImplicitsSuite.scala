package io.circe.optics.implicits

import io.circe.Json
import io.circe.optics.JsonPath._
import io.circe.syntax._
import io.circe.tests.CirceSuite

class JsonImplicitsSuite extends CirceSuite {

  import io.circe.optics.implicits._

  "findPathsToKey" should "find paths" in {
    forAll { (key1: String, key2: String, value1: String, value2: String) =>
      whenever(key1 != key2) {
        val js = Json.obj(
          key1 -> value1.asJson,
          key2 -> value2.asJson,
        )

        assert(js.findPathsToKey(key1).size == 1)
        assert(js.findPathsToKey(key1).head.json.getOption(js).get == value1.asJson)

        assert(js.findPathsToKey(key2).size == 1)
        assert(js.findPathsToKey(key2).head.json.getOption(js).get == value2.asJson)
      }
    }
  }

  it should "find nested paths" in {
    forAll { (key1: String, key2: String, value1: String, value2: String) =>
      whenever(key1 != key2) {
        val js = Json.obj(
          key1 -> Json.obj(
            key1 -> value1.asJson,
            key2 -> value2.asJson
          ),
          key2 -> value2.asJson
        )

        assert(js.findPathsToKey(key1).size == 2)
        assert(js.findPathsToKey(key1).head.json.getOption(js).get == Json.obj(key1 -> value1.asJson, key2 -> value2.asJson))
        assert(js.findPathsToKey(key1)(1).json.getOption(js).get == value1.asJson)

        assert(js.findPathsToKey(key2).size == 2)
        assert(js.findPathsToKey(key2).head.json.getOption(js).get == value2.asJson)
        assert(js.findPathsToKey(key2)(1).json.getOption(js).get == value2.asJson)
      }
    }
  }

  it should "find nested paths in arrays" in {
    forAll { (arrayName: String, key1: String, key2: String, value1: String) =>
      whenever(Set(arrayName, key1, key2).size == 3) {
        val jsObj = Json.obj(
          key1 -> value1.asJson,
          arrayName -> Json.arr(
            Json.obj(
              key1 -> "array1".asJson,
              key2 -> "array2".asJson
            ),
            Json.obj(
              key2 -> "array3".asJson
            )
          )
        )

        assert(jsObj.findPathsToKey(key1).size == 2)
        assert(jsObj.findPathsToKey(key1).head.json.getOption(jsObj).get == value1.asJson)
        assert(jsObj.findPathsToKey(key1)(1).json.getOption(jsObj).get == "array1".asJson)

        assert(jsObj.findPathsToKey(key2).size == 2)
        assert(jsObj.findPathsToKey(key2).head.json.getOption(jsObj).get == "array2".asJson)
        assert(jsObj.findPathsToKey(key2)(1).json.getOption(jsObj).get == "array3".asJson)

        val jsArray = Json.arr(
          Json.obj(
            key1 -> "array1".asJson,
            key2 -> "array2".asJson
          ),
          Json.obj(
            key2 -> "array3".asJson
          )
        )

        assert(jsArray.findPathsToKey(key1).size == 1)
        assert(jsArray.findPathsToKey(key1).head.json.getOption(jsArray).get == "array1".asJson)

        assert(jsArray.findPathsToKey(key2).size == 2)
        assert(jsArray.findPathsToKey(key2).head.json.getOption(jsArray).get == "array2".asJson)
        assert(jsArray.findPathsToKey(key2)(1).json.getOption(jsArray).get == "array3".asJson)
      }
    }
  }

  "findParentPathsToKey" should "create paths to just the parent" in {
    forAll { (key1: String, key2: String, value: String) =>
      whenever(key1 != key2) {
        val js = Json.obj(
          key1 -> Json.obj(
            key2 -> value.asJson
          ),
          "array" -> Json.arr(
            Json.obj(
              "arrayKey" -> value.asJson
            )
          )
        )

        assert(js.findParentPathsToKey(key2).size == 1)
        assert(js.findParentPathsToKey(key2).head.json.getOption(js).get == Json.obj(key2 -> value.asJson))

        assert(js.findParentPathsToKey(key1).head.json.getOption(js).get == js)

        assert(js.findParentPathsToKey("array").head.json.getOption(js).get == js)
        assert(js.findParentPathsToKey("arrayKey").head.json.getOption(js).get == Json.obj("arrayKey" -> value.asJson))
      }
    }
  }

  "findPathsToKey with a max depth" should "traverse only to the max depth" in {
    forAll { (key: String, randomDepth: Int) =>
      whenever(randomDepth >= 0 && randomDepth <= 1024) {

        def key(i: Int) = s"key-$i"

        var path = root
        var json = Json.obj()
        (0 to randomDepth).foreach(i => {
          json = path.json.modify(js => Json.obj(key(i) -> i.asJson))(json)
          path = path.selectDynamic(key(i))
        })

        assert(json.findPathsToKey(key(randomDepth), randomDepth).head.json.getOption(json).get == randomDepth.asJson)
        if (randomDepth > 0) {
          assert(json.findPathsToKey(key(randomDepth), randomDepth - 1).head.json.getOption(json).get ==
            Json.obj(
              key(randomDepth) -> randomDepth.asJson
            )
          )
        }
      }
    }
  }
}
