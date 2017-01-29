# circe

[![Build status](https://img.shields.io/travis/circe/circe/master.svg)](https://travis-ci.org/circe/circe)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe/master.svg)](https://codecov.io/github/circe/circe)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-core_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.11)

circe is a JSON library for Scala (and [Scala.js][scala-js]).

Please see the [guide](https://circe.github.io/circe/) for more information
about why circe exists and how to use it.

## Community

### Adopters

Are you using circe? Please consider opening a pull request to list your organization here:

* [Project September](http://www.projectseptember.com) (using circe to exchange and store data within the platform and serve data using GraphQL with Sangria)
* [Reonomy](https://reonomy.com/)
* [SoundCloud](https://www.soundcloud.com) (transforming 200,000,000 JSON events every hour in MapReduce ETLs)
* [TabMo](http://tabmo-group.io/) (parsing more than 100k events per second with Akka Stream and Spark)
* [Twilio](https://www.twilio.com) (sending many, many millions of messages a day with Circe and Akka)
* [The Guardian](https://www.theguardian.com)
* [Ravel Law](http://ravellaw.com/technology/) (using circe to (de)serialize data for search, analytics, and visualization of tens of millions of legal opinions)

### Other circe organization projects

Please get in touch on [Gitter][gitter] if you have a circe-related project that you'd like to discuss hosting under the
[circe organization][circe-org] on GitHub.

* [circe-jackson][circe-jackson]: A library that provides [Jackson][jackson]-supported parsing and printing for circe.
* [circe-yaml][circe-yaml]: A library that uses [SnakeYAML][snakeyaml] to support parsing YAML 1.1
  into circe's `Json`.
* [circe-spray][circe-spray]: A library that provides JSON marshallers and unmarshallers for [Spray][spray] using circe.
* [circe-benchmarks][circe-benchmarks]: Benchmarks for comparing the performance of circe and other JSON libraries for the JVM.

### Related projects

The following open source projects are either built on circe or provide circe support:

* [Actor Messenger][actor-im]: A platform for instant messaging.
* [akka-http-json][akka-http-json]: A library that supports using circe for JSON marshalling and
  unmarshalling in [Akka HTTP][akka-http].
* [akka-stream-json][akka-stream-json]: A library that provides Json support for stream based applications using jawn as a parser with a convenience example for circe.
* [Argus][argus]: Generates models and circe encoders and decoders from JSON schemas.
* [cornichon][cornichon]: A DSL for JSON API testing.
* [Cosmos][cosmos]: An API for [DCOS][dcos] services that uses circe.
* [crjdt][crjdt]: A conflict-free replicated JSON datatype in Scala.
* [diffson][diffson]: A Scala diff / patch library for JSON.
* [elastic4s][elastic4s]: A Scala client for [Elasticsearch][elasticsearch] with circe support.
* [Enumeratum][enumeratum]: Enumerations for Scala with circe integration.
* [Featherbed][featherbed]: A REST client library with circe support.
* [Finch][finch]: A library for building web services with circe support.
* [fintrospect][fintrospect]: HTTP contracts for [Finagle][finagle] with circe support.
* [fluflu][fluflu]: A [Fluentd][fluentd] logger.
* [Github4s][github4s]: A GitHub API wrapper written in Scala.
* [content-api-models][guardian-content-api-models]: The Guardian's Content API Thrift models.
* [http4s][http4s]: A purely functional HTTP library for client and server applications.
* [Iglu Schema Repository][iglu]: A [JSON Schema][json-schema] repository with circe support.
* [jsactor][jsactor]: An actor library for Scala.js with circe support.
* [jwt-circe][jwt-circe]: A [JSON Web Token][jwt] implementation with circe support.
* [kadai-log][kadai-log]: A logging library with circe support.
* [msgpack4z-circe][msgpack4z-circe]: A [MessagePack][msgpack] implementation with circe support.
* [play-circe][play-circe]: circe support for [Play!][play].
* [Rapture][rapture]: Support for using circe's parsing and AST in Rapture JSON.
* [roc][roc]: A PostgreSQL client built on Finagle.
* [sangria-circe][sangria-circe]: circe marshalling for [Sangria][sangria], a [GraphQL][graphql]
  implementation.
* [scalist][scalist]: A [Todoist][todoist] API client.
* [scala-jsonapi][scala-jsonapi]:  Scala support library for integrating the JSON API spec with Spray, Play! or Circe
* [Slick-pg][slick-pg]: [Slick][slick] extensions for PostgreSQL.
* [telepooz][telepooz]: A Scala wrapper for the [Telegram Bot API][telegram-bot-api] built on circe.
* [Zenith][zenith]: Functional HTTP library built on circe.

### Examples

The following projects provide examples, templates, or benchmarks that include circe:

* https://github.com/alanphillips78/akka-http-microservice-blueprint
* https://github.com/bneil/fcs_boilerplate
* https://github.com/gvolpe/simple-http4s-api
* https://github.com/notvitor/akka-http-circe-json-template
* https://github.com/stephennancekivell/some-jmh-json-benchmarks-circe-jackson

## Contributors and participation

circe is a fork of [Argonaut][argonaut], and if you find it at all useful, you should thank
[Mark Hibberd][markhibberd], [Tony Morris][tonymorris], [Kenji Yoshida][xuwei-k], and the rest of
the [Argonaut contributors][argonaut-contributors].

circe is currently maintained by [Travis Brown][travisbrown], [Alexandre Archambault][archambault],
and [Vladimir Kostyukov][vkostyukov]. After the 1.0 release, all pull requests will require two
sign-offs by a maintainer to be merged.

The circe project supports the [Typelevel][typelevel] [code of conduct][code-of-conduct] and wants
all of its channels (Gitter, GitHub, etc.) to be welcoming environments for everyone.

Please see the [contributors' guide](CONTRIBUTING.md) for details on how to submit a pull request.

## License

circe is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[actor-im]: https://actor.im/
[akka-http]: http://doc.akka.io/docs/akka/current/scala/http/
[akka-http-json]: https://github.com/hseeberger/akka-http-json
[akka-stream-json]: https://github.com/knutwalker/akka-stream-json
[apache]: http://www.apache.org/licenses/LICENSE-2.0
[archambault]: https://twitter.com/alxarchambault
[argonaut]: http://argonaut.io/
[argonaut-contributors]: https://github.com/argonaut-io/argonaut/graphs/contributors
[argus]: https://github.com/aishfenton/Argus
[circe-benchmarks]: https://github.com/circe/circe-benchmarks
[circe-jackson]: https://github.com/circe/circe-jackson
[circe-org]: https://github.com/circe
[circe-spray]: https://github.com/circe/circe-spray
[circe-yaml]: https://github.com/circe/circe-yaml
[crjdt]: https://github.com/fthomas/crjdt
[code-of-conduct]: http://typelevel.org/conduct.html
[cornichon]: https://github.com/agourlay/cornichon
[cosmos]: https://github.com/dcos/cosmos
[dcos]: https://dcos.io/
[diffson]: https://github.com/gnieh/diffson
[elastic4s]: https://github.com/sksamuel/elastic4s
[elasticsearch]: https://www.elastic.co/
[enumeratum]: https://github.com/lloydmeta/enumeratum
[featherbed]: https://github.com/finagle/featherbed
[finagle]: https://twitter.github.io/finagle/
[finch]: https://github.com/finagle/finch
[fintrospect]: https://github.com/daviddenton/fintrospect
[fluentd]: http://www.fluentd.org/
[fluflu]: https://github.com/tkrs/fluflu
[github4s]: https://github.com/47deg/github4s
[gitter]: https://gitter.im/circe/circe
[guardian-content-api-models]: https://github.com/guardian/content-api-models
[http4s]: https://github.com/http4s/http4s
[iglu]: https://github.com/snowplow/iglu
[jackson]: https://github.com/FasterXML/jackson
[jsactor]: https://github.com/codemettle/jsactor
[json-schema]: http://json-schema.org/
[jwt]: https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-32
[jwt-circe]: http://pauldijou.fr/jwt-scala/samples/jwt-circe/
[kadai-log]: https://bitbucket.org/atlassian/kadai-log
[markhibberd]: https://github.com/markhibberd
[msgpack]: https://github.com/msgpack/msgpack/blob/master/spec.md
[msgpack4z-circe]: https://github.com/msgpack4z/msgpack4z-circe
[play]: https://www.playframework.com/
[play-circe]: https://github.com/jilen/play-circe
[graphql]: http://graphql.org/docs/getting-started/
[rapture]: http://rapture.io/
[roc]: https://github.com/finagle/roc
[sangria]: http://sangria-graphql.org/
[sangria-circe]: https://github.com/sangria-graphql/sangria-circe
[scala-js]: http://www.scala-js.org/
[scala-jsonapi]: https://github.com/zalando/scala-jsonapi
[scalist]: https://github.com/vpavkin/scalist
[slick]: http://slick.lightbend.com/
[slick-pg]: https://github.com/tminglei/slick-pg
[snakeyaml]: https://bitbucket.org/asomov/snakeyaml
[spray]: http://spray.io/
[telegram-bot-api]: https://core.telegram.org/bots/api
[telepooz]: https://github.com/nikdon/telepooz
[todoist]: https://developer.todoist.com/
[tonymorris]: https://github.com/tonymorris
[travisbrown]: https://twitter.com/travisbrown
[typelevel]: http://typelevel.org/
[vkostyukov]: https://twitter.com/vkostyukov
[xuwei-k]: https://github.com/xuwei-k
[zenith]: https://github.com/sungiant/zenith
