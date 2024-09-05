# circe

[![Build status](https://img.shields.io/github/actions/workflow/status/circe/circe/ci.yml)](https://github.com/circe/circe/actions)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe/master.svg)](https://codecov.io/github/circe/circe)
![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-core_2.13?versionPrefix=0.14)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/XF3CXcMzqD)

circe is a JSON library for Scala (and [Scala.js][scala-js]).

Please see the [guide](https://circe.github.io/circe/) for more information
about why circe exists and how to use it.

## Community

### Adopters

Are you using circe? Please consider opening a pull request to list your organization here:

* [Abacus](https://abacusprotocol.com/)
* [Anduin Transactions](https://anduintransact.com/)
* [Apollo Agriculture](https://apolloagriculture.com/)
* [AutoScout24](https://www.autoscout24.com/)
* [Babylon Health](https://www.babylonhealth.com/)
* [Banno inside of Jack Henry](https://banno.com/)
* [BBC](http://www.bbc.co.uk)
* [Broad Institute](https://www.broadinstitute.org/data-sciences-platform)
* [Chartboost](https://www.chartboost.com/) (sending hundreds of thousands of messages per second on our Ad Exchange)
* [CiBO Technologies](http://www.cibotechnologies.com) (using circe to (de)serialize data in support of a sustainable revolution in agriculture)
* [ClearScore](https://www.clearscore.com)
* [Codacy](https://www.codacy.com)
* [Colisweb](https://www.colisweb.com)
* [Connio](https://www.connio.com) (creating and managing digital twins with Circe and Akka)
* [Coya](https://www.coya.com/)
* [Criteo](https://www.criteo.com/) ([collecting 200.000 events per second from our banners](https://medium.com/criteo-labs/migrate-a-service-getting-200kqps-from-jackson-to-circe-a475b2718206))
* [Dassault Systèmes](https://www.3ds.com/)
* [Datalogue](https://datalogue.io)
* [Dreamlines](https://www.dreamlines.com/)
* [DriveTribe](https://drivetribe.com)
* [Earnest](https://www.earnest.com)
* [Elastic](https://www.elastic.co)
* [Emmy Sharing](https://emmy-sharing.de/en/)
* [FOLIO](https://folio-sec.com/)
* [GLNGN Server](https://glngn.com)
* [Gympass](https://www.gympass.com/)
* [Gutefrage](https://www.gutefrage.net)
* [Hello Soda](https://hellosoda.com/)
* [HERE Technologies](https://www.here.com/)
* [Hireproof](https://hireproof.io/)
* [HolidayCheck](https://www.holidaycheck.de)
* [ImmobilienScout24](https://www.immobilienscout24.de/)
* [Immozentral](https://www.immozentral.com/)
* [Indix](https://www.indix.com)
* [ITV](https://www.itv.com/)
* [Kinoplan](https://kinoplan.ru/)
* [Latitude Financial Services](https://www.latitudefinancial.com.au/)
* [MatchesFashion](https://www.matchesfashion.com)
* [Merit](https://merits.com)
* [Metacommerce](https://www.metacommerce.ru)
* [Mobile GmbH](https://www.mobile.de)
* [MomentFeed](https://momentfeed.com)
* [New Work](https://www.new-work.se/en/)
* [Nike](https://www.nike.com)
* [Ocado Technology](https://ocadotechnology.com)
* [On Air Entertainment](https://onairentertainment.com/)
* [Onfocus](https://onfocus.io)
* [Opt Technologies](https://opt-technologies.jp/)
* [OVO Energy](https://www.ovoenergy.com)
* [Panaseer](https://panaseer.com)
* [Permutive](http://permutive.com)
* [Prezi](https://prezi.com)
* [Project September](http://www.projectseptember.com) (using circe to exchange and store data within the platform and serve data using GraphQL with Sangria)
* [Raster Foundry](https://github.com/raster-foundry/raster-foundry/)
* [Ravel Law](http://ravellaw.com/technology/) (using circe to (de)serialize data for search, analytics, and visualization of tens of millions of legal opinions)
* [REA Group - realestate.com.au](https://www.realestate.com.au/)
* [Reonomy](https://reonomy.com/)
* [Resilient plc](https://resilientplc.com/)
* [Sky](https://www.sky.com/)
* [Snowplow Analytics](https://snowplowanalytics.com/)
* [SoundCloud](https://www.soundcloud.com) (transforming 200,000,000 JSON events every hour in MapReduce ETLs)
* [Spotify](https://www.spotify.com) (using circe for JSON IO in [Scio][scio])
* [SpotX](https://www.spotx.tv/)
* [Stripe](https://stripe.com)
* [Stylight](https://stylight.de)
* [TabMo](http://tabmo-group.io/) (parsing more than 100k events per second with Akka Stream and Spark)
* [Tinkoff](https://tinkoff.ru/)
* [The Guardian](https://www.theguardian.com)
* [Threat Stack](https://www.threatstack.com/)
* [Tranzzo](https://tranzzo.com/)
* [Twilio](https://www.twilio.com) (sending many, many millions of messages a day with Circe and Akka)
* [Uberall](https://stackshare.io/uberall/uberall)
* [VEACT](https://veact.net/)
* [WeWork](https://www.wework.com)
* [Whisk](https://whisk.com)
* [Wide Angle Analytics](https://wideangle.co)
* [Zalando](https://zalando.de)
* [Zendesk](https://zendesk.com)

### Other circe organization projects

Please get in touch on [Gitter][gitter] if you have a circe-related project that you'd like to discuss hosting under the
[circe organization][circe-org] on GitHub.

* [circe-benchmarks][circe-benchmarks]: Benchmarks for comparing the performance of circe and other JSON libraries for the JVM.
* [circe-config][circe-config]: A library for translating between HOCON, Java properties, and JSON documents.
* [circe-derivation][circe-derivation]: Experimental generic derivation with improved compile times.
* [circe-fs2][circe-fs2]: A library that provides streaming JSON parsing and decoding built on [fs2][fs2] and [Jawn][jawn].
* [circe-iteratee][circe-iteratee]: A library that provides streaming JSON parsing and decoding built on [iteratee.io][iteratee] and [Jawn][jawn].
* [circe-jackson][circe-jackson]: A library that provides [Jackson][jackson]-supported parsing and printing for circe.
* [circe-spray][circe-spray]: A library that provides JSON marshallers and unmarshallers for [Spray][spray] using circe.
* [circe-yaml][circe-yaml]: A library that uses [SnakeYAML][snakeyaml] to support parsing YAML 1.1
  into circe's `Json`.
* [circe-refined][circe-refined]: A library that provides encoders and decoders using [Refined][refined].

### Related projects

The following open source projects are either built on circe or provide circe support:

* [Actor Messenger][actor-im]: A platform for instant messaging.
* [akka-http-json][akka-http-json]: A library that supports using circe for JSON marshalling and
  unmarshalling in [Akka HTTP][akka-http].
* [akka-stream-json][akka-stream-json]: A library that provides JSON support for stream based applications using Jawn as a parser with a convenience example for circe.
* [Argus][argus]: Generates models and circe encoders and decoders from JSON schemas.
* [Blackdoor JOSE][jose-json-circe]: circe JSON support for blackdoor JOSE and JWT.
* [borer][borer]: Allows circe encoders/decoders to be reused for CBOR (de)serialization.
* [circe-debezium][circe-debezium]: Circe codecs for [Debezium][debezium] payload types
* [circe-geojson][circe-geojson]: Circe support for GeoJSON (RFC 7946)
* [circe-kafka][circe-kafka]: Implicit conversion of Encoder and Decoder into Kafka Serializer/Deserializer/Serde
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
* [IdeaLingua][izumi-r2]: Staged Interface Definition and Data Modeling Language & RPC system currently targeting Scala, Go, C# and TypeScript. Scala codegen generates models and JSON codecs using circe.
* [Iglu Schema Repository][iglu]: A [JSON Schema][json-schema] repository with circe support.
* [jsactor][jsactor]: An actor library for Scala.js with circe support.
* [jsoniter-scala-circe][jsoniter-scala-circe]: A booster for faster parsing/printing to/from circe AST and decoding/encoding of `java.time._` and `BigInt` types.
* [jwt-circe][jwt-circe]: A [JSON Web Token][jwt] implementation with circe support.
* [kadai-log][kadai-log]: A logging library with circe support.
* [msgpack4z-circe][msgpack4z-circe]: A [MessagePack][msgpack] implementation with circe support.
* [ohNoMyCirce][ohNoMyCirce]: Friendly compile error messages for [shapeless][shapeless]'s Generic, [circe][circe-org]'s Encoder & Decoder and [slick][slick]'s case class mapping.
* [pekko-http-json][pekko-http-json]: A library that supports using circe for JSON marshalling and
  unmarshalling in [Pekko HTTP][pekko-http].
* [pekko-streams-circe][pekko-streams-circe]: A library that provides JSON support for stream based applications using Jawn as a parser with a convenience example for circe.
* [play-circe][play-circe]: circe support for [Play!][play].
* [pulsar4s][pulsar4s]: A Scala client for [Apache-Pulsar][pulsar] with circe support.
* [Rapture][rapture]: Support for using circe's parsing and AST in Rapture JSON.
* [roc][roc]: A PostgreSQL client built on Finagle.
* [sangria-circe][sangria-circe]: circe marshalling for [Sangria][sangria], a [GraphQL][graphql]
  implementation.
* [scalist][scalist]: A [Todoist][todoist] API client.
* [scala-jsonapi][scala-jsonapi]:  Scala support library for integrating the JSON API spec with Spray, Play! or Circe
* [scala-json-rpc]: [JSON-RPC][json-rpc] 2.0 library for Scala and Scala.js 
* [scalatest-json-circe]: Scalatest matchers for Json with appropriate equality and descriptive error messages.
* [Scio][scio]: A Scala API for Apache Beam and Google Cloud Dataflow, uses circe for JSON IO
* [seals][seals]: Tools for schema evolution and language-integrated schemata (derives circe encoders and decoders).
* [shaclex][shaclex]: RDF validation using SHACL or ShEx. 
* [Slick-pg][slick-pg]: [Slick][slick] extensions for PostgreSQL.
* [sttp][sttp]: Scala HTTP client.
* [Synapses][synapses]: A lightweight Neural Network library, for js, jvm and .net.
* [telepooz][telepooz]: A Scala wrapper for the [Telegram Bot API][telegram-bot-api] built on circe.
* [Zenith][zenith]: Functional HTTP library built on circe.

### Examples

The following projects provide examples, templates, or benchmarks that include circe:

* https://github.com/alanphillips78/akka-http-microservice-blueprint
* https://github.com/bneil/fcs_boilerplate
* https://github.com/gvolpe/simple-http4s-api
* https://github.com/vitorsvieira/akka-http-circe-json-template
* https://github.com/stephennancekivell/some-jmh-json-benchmarks-circe-jackson
* https://github.com/pauljamescleary/scala-pet-store

## Contributors and participation

circe is a fork of [Argonaut][argonaut], and if you find it at all useful, you should thank
[Mark Hibberd][markhibberd], [Tony Morris][tonymorris], [Kenji Yoshida][xuwei-k], and the rest of
the [Argonaut contributors][argonaut-contributors].

circe is currently maintained by [Darren Gibson][zarthross] and [Erlend Hamnaberg][hamnis]. 
After the 1.0 release, all pull requests will require two
sign-offs by a maintainer to be merged.

The circe project is a typelevel affiliate project, and follow the [Typelevel Code of Conduct](CODE_OF_CONDUCT.md)

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
[borer]: https://sirthias.github.io/borer/
[circe-benchmarks]: https://github.com/circe/circe-benchmarks
[circe-config]: https://github.com/circe/circe-config
[circe-debezium]: https://github.com/compstak/circe-debezium
[circe-derivation]: https://github.com/circe/circe-derivation
[circe-fs2]: https://github.com/circe/circe-fs2
[circe-iteratee]: https://github.com/circe/circe-iteratee
[circe-jackson]: https://github.com/circe/circe-jackson
[circe-geojson]: https://github.com/compstak/circe-geojson
[circe-kafka]: https://github.com/NeQuissimus/circe-kafka
[circe-org]: https://github.com/circe
[circe-spray]: https://github.com/circe/circe-spray
[circe-yaml]: https://github.com/circe/circe-yaml
[circe-refined]: https://github.com/circe/circe-refined
[crjdt]: https://github.com/fthomas/crjdt
[code-of-conduct]: https://www.scala-lang.org/conduct/
[cornichon]: https://github.com/agourlay/cornichon
[cosmos]: https://github.com/dcos/cosmos
[dcos]: https://dcos.io/
[debezium]: https://debezium.io/
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
[fs2]: https://github.com/functional-streams-for-scala/fs2
[github4s]: https://github.com/47deg/github4s
[gitter]: https://gitter.im/circe/circe
[guardian-content-api-models]: https://github.com/guardian/content-api-models
[hamnis]: https://github.com/hamnis
[http4s]: https://github.com/http4s/http4s
[iteratee]: https://github.com/travisbrown/iteratee
[iglu]: https://github.com/snowplow/iglu
[izumi-r2]: https://github.com/pshirshov/izumi-r2
[jackson]: https://github.com/FasterXML/jackson
[jawn]: https://github.com/non/jawn
[jose-json-circe]: https://github.com/blackdoor/jose
[jsactor]: https://github.com/codemettle/jsactor
[json-schema]: http://json-schema.org/
[json-rpc]: http://www.jsonrpc.org
[jsoniter-scala-circe]: https://github.com/plokhotnyuk/jsoniter-scala/tree/master/jsoniter-scala-circe
[jwt]: https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-32
[jwt-circe]: http://pauldijou.fr/jwt-scala/samples/jwt-circe/
[kadai-log]: https://bitbucket.org/atlassian/kadai-log
[markhibberd]: https://github.com/markhibberd
[msgpack]: https://github.com/msgpack/msgpack/blob/master/spec.md
[msgpack4z-circe]: https://github.com/msgpack4z/msgpack4z-circe
[ohNoMyCirce]: https://github.com/djx314/ohNoMyCirce
[pekko-http]: https://pekko.apache.org/docs/pekko-http/current/
[pekko-http-json]: https://github.com/pjfanning/pekko-http-json
[pekko-streams-circe]: https://github.com/mdedetrich/pekko-streams-circe
[play]: https://www.playframework.com/
[play-circe]: https://github.com/jilen/play-circe
[pulsar]: https://pulsar.apache.org/
[pulsar4s]: https://github.com/sksamuel/pulsar4s
[graphql]: http://graphql.org/docs/getting-started/
[rapture]: http://rapture.io/
[refined]: https://github.com/fthomas/refined
[roc]: https://github.com/finagle/roc
[sangria]: http://sangria-graphql.org/
[sangria-circe]: https://github.com/sangria-graphql/sangria-circe
[scala-js]: http://www.scala-js.org/
[scala-jsonapi]: https://github.com/scala-jsonapi/scala-jsonapi
[scala-json-rpc]: https://github.com/shogowada/scala-json-rpc
[scalatest-json-circe]: https://github.com/stephennancekivell/scalatest-json
[scalist]: https://github.com/vpavkin/scalist
[scio]: https://github.com/spotify/scio
[seals]: https://github.com/durban/seals/
[shapeless]: https://github.com/milessabin/shapeless
[shaclex]: https://github.com/labra/shaclex
[slick]: http://slick.lightbend.com/
[slick-pg]: https://github.com/tminglei/slick-pg
[snakeyaml]: https://bitbucket.org/asomov/snakeyaml
[spray]: http://spray.io/
[sttp]: https://github.com/softwaremill/sttp
[synapses]: https://mrdimosthenis.github.io/Synapses
[telegram-bot-api]: https://core.telegram.org/bots/api
[telepooz]: https://github.com/nikdon/telepooz
[todoist]: https://developer.todoist.com/
[tonymorris]: https://github.com/tonymorris
[travisbrown]: https://twitter.com/travisbrown
[typelevel]: http://typelevel.org/
[vkostyukov]: https://twitter.com/vkostyukov
[xuwei-k]: https://github.com/xuwei-k
[zenith]: https://github.com/sungiant/zenith
[zarthross]: https://github.com/zarthross/
[zmccoy]: https://github.com/zmccoy/
