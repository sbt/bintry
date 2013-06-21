# bintry

your packages, delivered fresh

A scala interface for the [bintray](https://bintray.com) [api](https://bintray.com/docs/rest/api.html).

# install

## copy and paste method

add the following to your sbt build definition

    libraryDependencies += "me.lessis" %% "bintry" % "0.1.0"

## civilized method

using [ls](https://github.com/softprops/ls#readme)

    ls-install bintry

# usage

```scala
import bintry._, dispatch._, dispatch.Defaults._, org.json4s._
val bty = Client(user, apikey)
val repo = bty.repo(user, "generic")
```

## create a package

```scala
repo.createPackage("foo", "blah")(as.json4s.Json)
```


## create a version

```scala
repo.get("foo").createVersion("0.1.0")(as.json4s.Json)
```

## upload & publish it

```scala
repo.get("foo").version("0.1.0")
               .upload("/baz", file("foo_2.10"),
                      publish = true)(as.json4s.Json)
```
Doug Tangren (softprops) 2013
