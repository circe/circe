---
layout: default
title:  "Performance"
section: "performance"
---

# Performance

circe aims to be more focused on performance. I'm still experimenting with the right balance, but I'm
open to using mutability, inheritance, and all kinds of other horrible things under the hood if they
make circe faster (the public API does not and will never expose any of this, though).

[My initial benchmarks][benchmarks] suggest this is at least kind of working (higher numbers are
better):

```
Benchmark                          Mode  Cnt        Score       Error  Units
DecodingBenchmark.decodeFoosC     thrpt   40     3711.680 ±    22.766  ops/s
DecodingBenchmark.decodeFoosA     thrpt   40     1519.045 ±    11.373  ops/s
DecodingBenchmark.decodeFoosP     thrpt   40     2032.834 ±    27.033  ops/s
DecodingBenchmark.decodeFoosPico  thrpt   40     2003.106 ±    10.463  ops/s
DecodingBenchmark.decodeFoosS     thrpt   40     7053.699 ±    35.127  ops/s

DecodingBenchmark.decodeIntsC     thrpt   40    19101.875 ±   324.123  ops/s
DecodingBenchmark.decodeIntsA     thrpt   40     8000.093 ±   215.702  ops/s
DecodingBenchmark.decodeIntsP     thrpt   40    18160.031 ±    68.777  ops/s
DecodingBenchmark.decodeIntsPico  thrpt   40    11979.085 ±    89.793  ops/s
DecodingBenchmark.decodeIntsS     thrpt   40    81279.228 ±  1203.751  ops/s

EncodingBenchmark.encodeFoosC     thrpt   40     7353.158 ±   133.633  ops/s
EncodingBenchmark.encodeFoosA     thrpt   40     5638.358 ±    30.315  ops/s
EncodingBenchmark.encodeFoosP     thrpt   40     2324.075 ±    17.868  ops/s
EncodingBenchmark.encodeFoosPico  thrpt   40     5056.317 ±    45.876  ops/s
EncodingBenchmark.encodeFoosS     thrpt   40     5307.422 ±    29.666  ops/s

EncodingBenchmark.encodeIntsC     thrpt   40   117885.093 ±  2151.059  ops/s
EncodingBenchmark.encodeIntsA     thrpt   40    72986.276 ±  1561.295  ops/s
EncodingBenchmark.encodeIntsP     thrpt   40    55117.582 ±   650.154  ops/s
EncodingBenchmark.encodeIntsPico  thrpt   40    31602.757 ±   351.578  ops/s
EncodingBenchmark.encodeIntsS     thrpt   40    40509.667 ±   560.439  ops/s

ParsingBenchmark.parseFoosC       thrpt   40     2869.779 ±    61.898  ops/s
ParsingBenchmark.parseFoosA       thrpt   40     2615.299 ±    25.881  ops/s
ParsingBenchmark.parseFoosP       thrpt   40     1970.493 ±    90.383  ops/s
ParsingBenchmark.parseFoosPico    thrpt   40     3113.232 ±    29.081  ops/s
ParsingBenchmark.parseFoosS       thrpt   40     3725.056 ±    68.794  ops/s

ParsingBenchmark.parseIntsC       thrpt   40    13062.151 ±   209.713  ops/s
ParsingBenchmark.parseIntsA       thrpt   40    11066.850 ±   159.308  ops/s
ParsingBenchmark.parseIntsP       thrpt   40    18980.265 ±    91.351  ops/s
ParsingBenchmark.parseIntsPico    thrpt   40    15184.314 ±    37.808  ops/s
ParsingBenchmark.parseIntsS       thrpt   40    15495.935 ±   388.922  ops/s

PrintingBenchmark.printFoosC      thrpt   40     4090.218 ±    38.804  ops/s
PrintingBenchmark.printFoosA      thrpt   40     2863.570 ±    19.091  ops/s
PrintingBenchmark.printFoosP      thrpt   40     9042.816 ±    49.199  ops/s
PrintingBenchmark.printFoosPico   thrpt   40     4759.601 ±    20.467  ops/s
PrintingBenchmark.printFoosS      thrpt   40     7297.047 ±    28.168  ops/s

PrintingBenchmark.printIntsC      thrpt   40    24596.715 ±    66.366  ops/s
PrintingBenchmark.printIntsA      thrpt   40    15611.121 ±   140.017  ops/s
PrintingBenchmark.printIntsP      thrpt   40    66283.874 ±   731.534  ops/s
PrintingBenchmark.printIntsPico   thrpt   40    23703.796 ±   188.186  ops/s
PrintingBenchmark.printIntsS      thrpt   40    53015.753 ±   462.472  ops/s
```

And allocation rates (lower is better):

```
Benchmark                                              Mode  Cnt        Score        Error   Units

DecodingBenchmark.decodeFoosC:gc.alloc.rate.norm      thrpt   20  1308424.455 ±      0.881    B/op
DecodingBenchmark.decodeFoosA:gc.alloc.rate.norm      thrpt   20  3779097.640 ±      2.456    B/op
DecodingBenchmark.decodeFoosP:gc.alloc.rate.norm      thrpt   20  2201336.820 ±      1.588    B/op
DecodingBenchmark.decodeFoosPico:gc.alloc.rate.norm   thrpt   20   506696.832 ±      1.608    B/op
DecodingBenchmark.decodeFoosS:gc.alloc.rate.norm      thrpt   20   273184.238 ±      0.458    B/op

DecodingBenchmark.decodeIntsC:gc.alloc.rate.norm      thrpt   20   291360.090 ±      0.174    B/op
DecodingBenchmark.decodeIntsA:gc.alloc.rate.norm      thrpt   20   655448.200 ±      0.387    B/op
DecodingBenchmark.decodeIntsP:gc.alloc.rate.norm      thrpt   20   369144.097 ±      0.189    B/op
DecodingBenchmark.decodeIntsPico:gc.alloc.rate.norm   thrpt   20   235400.144 ±      0.280    B/op
DecodingBenchmark.decodeIntsS:gc.alloc.rate.norm      thrpt   20    38136.021 ±      0.041    B/op

EncodingBenchmark.encodeFoosC:gc.alloc.rate.norm      thrpt   20   395272.225 ±      0.433    B/op
EncodingBenchmark.encodeFoosA:gc.alloc.rate.norm      thrpt   20   521136.306 ±      0.595    B/op
EncodingBenchmark.encodeFoosP:gc.alloc.rate.norm      thrpt   20  1367800.719 ±      7.263    B/op
EncodingBenchmark.encodeFoosPico:gc.alloc.rate.norm   thrpt   20   281992.346 ±      0.674    B/op
EncodingBenchmark.encodeFoosS:gc.alloc.rate.norm      thrpt   20   377856.318 ±      0.615    B/op

EncodingBenchmark.encodeIntsC:gc.alloc.rate.norm      thrpt   20    64160.016 ±      7.129    B/op
EncodingBenchmark.encodeIntsA:gc.alloc.rate.norm      thrpt   20    80152.023 ±      0.044    B/op
EncodingBenchmark.encodeIntsP:gc.alloc.rate.norm      thrpt   20    71352.030 ±      0.058    B/op
EncodingBenchmark.encodeIntsPico:gc.alloc.rate.norm   thrpt   20    58992.057 ±      0.115    B/op
EncodingBenchmark.encodeIntsS:gc.alloc.rate.norm      thrpt   20    76176.042 ±      0.081    B/op

ParsingBenchmark.parseFoosC:gc.alloc.rate.norm        thrpt   20   765800.586 ±      1.133    B/op
ParsingBenchmark.parseFoosA:gc.alloc.rate.norm        thrpt   20  1488760.635 ±      1.228    B/op
ParsingBenchmark.parseFoosP:gc.alloc.rate.norm        thrpt   20   987720.805 ±      1.551    B/op
ParsingBenchmark.parseFoosPico:gc.alloc.rate.norm     thrpt   20   639464.525 ±      1.014    B/op
ParsingBenchmark.parseFoosS:gc.alloc.rate.norm        thrpt   20   252256.440 ±      0.838    B/op

ParsingBenchmark.parseIntsC:gc.alloc.rate.norm        thrpt   20   121272.129 ±      0.250    B/op
ParsingBenchmark.parseIntsA:gc.alloc.rate.norm        thrpt   20   310280.151 ±      0.289    B/op
ParsingBenchmark.parseIntsP:gc.alloc.rate.norm        thrpt   20   216448.089 ±      0.171    B/op
ParsingBenchmark.parseIntsPico:gc.alloc.rate.norm     thrpt   20   141808.118 ±      0.239    B/op
ParsingBenchmark.parseIntsS:gc.alloc.rate.norm        thrpt   20   109000.117 ±      0.229    B/op

PrintingBenchmark.printFoosC:gc.alloc.rate.norm       thrpt   20   425240.419 ±      0.810    B/op
PrintingBenchmark.printFoosA:gc.alloc.rate.norm       thrpt   20   621288.585 ±   1069.068    B/op
PrintingBenchmark.printFoosP:gc.alloc.rate.norm       thrpt   20   351360.184 ±      0.356    B/op
PrintingBenchmark.printFoosPico:gc.alloc.rate.norm    thrpt   20   431268.348 ±   1058.404    B/op
PrintingBenchmark.printFoosS:gc.alloc.rate.norm       thrpt   20   372992.228 ±      0.442    B/op

PrintingBenchmark.printIntsC:gc.alloc.rate.norm       thrpt   20    74464.067 ±      7.127    B/op
PrintingBenchmark.printIntsA:gc.alloc.rate.norm       thrpt   20   239712.107 ±      0.206    B/op
PrintingBenchmark.printIntsP:gc.alloc.rate.norm       thrpt   20    24144.025 ±      0.048    B/op
PrintingBenchmark.printIntsPico:gc.alloc.rate.norm    thrpt   20    95472.072 ±      0.140    B/op
PrintingBenchmark.printIntsS:gc.alloc.rate.norm       thrpt   20    24048.032 ±      0.062    B/op
```

The `Foos` benchmarks work with a map containing case class values, and the `Ints` ones are an array
of integers. `C` suffixes indicate circe's throughput, `A` is for Argonaut, `P` is for
[play-json][play-json], `Pico` is for [picopickle][picopickle], and `S` is for
[spray-json][spray-json]. Note that spray-json's approach to failure handling is different from the
approaches of the other libraries listed here (it simply throws exceptions), and this difference
should be taken into account when comparing its results with the others.

{% include references.md %}
