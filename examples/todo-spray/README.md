# Web services with Spray and circe

The [circe-spray][circe-spray] project makes it easy to create web services with [Spray][spray]
that use circe for JSON serialization. This project demonstrates a simple web
service with a single endpoint that accepts a posted "todo" object with three
fields and returns a completed todo value with a generated identifier.

Note that this example requires Java 8 and Scala 2.11.

To start the service locally on port 8080, run `sbt run`. If that works, you can
test the service from your favorite HTTP client (I'll use [httpie][httpie]
here):

```bash
$ http POST :8080/api/v1/todo title=Foo completed:=false order=0 dueDate=2007-12-03T10:15:30
HTTP/1.1 200 OK
Content-Length: 119
Content-Type: application/json; charset=UTF-8
Date: Sat, 23 Apr 2016 18:52:41 GMT
Server: spray-can/1.3.3

{
    "completed": false,
    "dueDate": "2007-12-03T10:15:30",
    "id": "bc0b7b19-8b3c-45e8-ab00-5b64bf1a0659",
    "order": 0,
    "title": "Foo"
}
```

If the posted JSON is missing required fields, the errors will be accumulated
and we'll get back a message listing all of them:

```bash
$ http POST :8080/api/v1/todo title=Foo
HTTP/1.1 400 Bad Request
Content-Length: 217
Content-Type: application/json; charset=UTF-8
Date: Sat, 23 Apr 2016 18:54:11 GMT
Server: spray-can/1.3.3

[
    "DecodingFailure at .completed: Attempt to decode value on failed cursor",
    "DecodingFailure at .order: Attempt to decode value on failed cursor",
    "DecodingFailure at .dueDate: Attempt to decode value on failed cursor"
]
```

This all works without us having to write a single line of code describing how
to decode or encode our `Todo` type as JSON.

[circe-spray]: https://github.com/circe/circe-spray
[httpie]: https://github.com/jkbrzt/httpie
[spray]: http://spray.io/
