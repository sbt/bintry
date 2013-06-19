# bintry

your packages, delivered fresh

A jvm interface for the [bintray](https://bintray.com) [api](https://bintray.com/docs/rest/api.html).

# usage

```scala
import bintry._, dispatch._, dispatch.Defaults._, org.json4s._
val bty = Client(user, apikey)
val repo = bty.repo(user, "generic")
for {
  // create a package
  pkg <- repo.createPackage("foo", "blah")(as.json4s.Json)
  // create a version
  ver   <- repo.get("foo").createVersion("0.1.0")(as.json4s.Json)
  // upload & publish it
  _   <- repo.get("foo").version("0.1.0")
                        .upload("/baz", file("foo_2.10"),
                                publish = true)(identity)
  // do with it what you will
  JObject(pfs) <- pkg
  ("name", JString(package)) <- pfs
  JObject(vfs) <- ver
  ("name", JString(version)) <- vfs
} yield {
  println("hell yeah you just released %s version %s"
            .format(package, version))
}
```

Doug Tangren (softprops) 2013
