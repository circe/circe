package io.circe.jawn;

import io.circe.Json;
import io.circe.JsonNumber;
import io.circe.JsonObject;
import java.util.LinkedHashMap;
import jawn.RawFacade;
import jawn.RawFContext;
import jawn.SupportParser;
import scala.collection.immutable.VectorBuilder;

final class CirceFacade implements RawFacade<Json> {
  final static private Json NULL = Json.Null();
  final static private Json FALSE = Json.False();
  final static private Json TRUE = Json.True();

  final public Json jnull(int index) {
    return NULL;
  }

  final public Json jfalse(int index) {
    return FALSE;
  }

  final public Json jtrue(int index) {
    return TRUE;
  }

  final public Json jnum(CharSequence s, int decIndex, int expIndex, int index) {
    JsonNumber number;

    if (decIndex < 0 && expIndex < 0) {
      number = JsonNumber.fromIntegralStringUnsafe(s.toString());
    } else {
      number = JsonNumber.fromDecimalStringUnsafe(s.toString());
    }

    return Json.fromJsonNumber(number);
  }

  final public Json jstring(CharSequence s, int index) {
    return Json.fromString(s.toString());
  }

  final public RawFContext<Json> singleContext(int index) {
    return new SingleContext();
  }

  final public RawFContext<Json> arrayContext(int index) {
    return new ArrayContext();
  }

  final public RawFContext<Json> objectContext(int index) {
    return new ObjectContext();
  }

  private final class SingleContext implements RawFContext<Json> {
    private Json value = null;

    final public void add(CharSequence s, int index) {
      this.value = jstring(s.toString(), index);
    }
    final public void add(Json v, int index) {
      this.value = v;
    }
    final public Json finish(int index) {
      return this.value;
    }
    final public boolean isObj() {
      return false;
    }
  }

  private final class ArrayContext implements RawFContext<Json> {
    final private VectorBuilder<Json> vs = new VectorBuilder<Json>();

    final public void add(CharSequence s, int index) {
      this.vs.$plus$eq(jstring(s.toString(), index));
    }
    final public void add(Json v, int index) {
       this.vs.$plus$eq(v);
    }
    final public Json finish(int index) {
      return Json.fromValues(this.vs.result());
    }
    final public boolean isObj() {
      return false;
    }
  }

  private final class ObjectContext implements RawFContext<Json> {
    private String key = null;
    private final LinkedHashMap<String, Json> m = new LinkedHashMap<String, Json>();

    final public void add(CharSequence s, int index) {
      if (key == null) {
        key = s.toString();
      } else {
        m.put(key, jstring(s, index));
        key = null;
      }
    }
    final public void add(Json v, int index) {
      m.put(key, v);
      key = null;
    }
    final public Json finish(int index) {
      return Json.fromJsonObject(JsonObject.fromLinkedHashMap(m));
    }
    final public boolean isObj() {
      return true;
    }
  }
}
