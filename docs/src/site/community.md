---
layout: default
title:  "Community"
section: "community"
---

# Community

## Related projects

The following open source projects are either built on circe or provide circe support:

* [Actor Messenger][actor-im]: A platform for instant messaging.
* [akka-http-json][akka-http-json]: A library that supports using circe for JSON marshalling and
  unmarshalling in [Akka HTTP][akka-http].
* [akka-stream-json][akka-stream-json]: A library that provides Json support for stream based applications 
  using jawn as a parser with a convenience example for circe.
* [circe-yaml][circe-yaml]: A library that uses [SnakeYAML][snakeyaml] to support parsing YAML 1.1
  into circe's `Json`.
* [cornichon][cornichon]: A DSL for JSON API testing.
* [Cosmos][cosmos]: An API for [DCOS][dcos] services that uses circe.
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
* [Slick-pg][slick-pg]: [Slick][slick] extensions for PostgreSQL.
* [telepooz][telepooz]: A Scala wrapper for the [Telegram Bot API][telegram-bot-api] built on circe.
* [Zenith][zenith]: Functional HTTP library built on [Unfiltered][unfiltered] and circe.
* [Argus][argus]: Generates models and Circe encoders/decoders from Json schemas.

## Examples

The following projects provide examples, templates, or benchmarks that include circe:

* [alanphillips78/akka-http-microservice-blueprint](https://github.com/alanphillips78/akka-http-microservice-blueprint)
* [bneil/fcs_boilerplate](https://github.com/bneil/fcs_boilerplate)
* [gvolpe/simple-http4s-api](https://github.com/gvolpe/simple-http4s-api)
* [notvitor/akka-http-circe-json-template](https://github.com/notvitor/akka-http-circe-json-template)
* [stephennancekivell/some-jmh-json-benchmarks-circe-jackson](https://github.com/stephennancekivell/some-jmh-json-benchmarks-circe-jackson)

## Adopters

Are you using circe? Please consider opening a pull request to list your organization here:

* [Reonomy](https://reonomy.com/)
* [SoundCloud](https://www.soundcloud.com) (transforming 200,000,000 JSON events every hour in MapReduce ETLs)
* [TabMo](http://tabmo-group.io/) (parsing more than 100k events per second with Akka Stream and Spark)
* [Twilio](https://www.twilio.com) (sending many, many millions of messages a day with Circe and Akka)
* [Project September](http://www.projectseptember.com) (using circe to exchange/store data within the platform and serve data using graphql with sangria)

{% include references.md %}
