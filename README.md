# bintry

your packages, delivered fresh

A scala interface for the [bintray](https://bintray.com) [api](https://bintray.com/docs/api/).

# install

## copy and paste method

add the following to your sbt build definition

```scala
libraryDependencies += "me.lessis" %% "bintry" % "0.4.0"
```

## civilized method

using [ls](https://github.com/softprops/ls#readme)

    ls-install bintry

# usage

Create a new bintry `Client` with your bintray username and api key ( which can be found [here](https://bintray.com/profile/edit) )

```scala
import bintry._, dispatch._, dispatch.Defaults._, org.json4s._
val bty = Client(user, apikey)
val repo = bty.repo(user, "generic")
```

## create a package

```scala
repo.createPackage("my-awesome-package",
                   "description of my awesome package",
                   Seq("MIT"))(as.json4s.Json)
```


## create a version

```scala
repo.get("my-awesome-package").createVersion("0.1.0")(as.json4s.Json)
```

## upload & publish it

```scala
repo.get("my-awesome-package").version("0.1.0")
               .upload("/baz", file("foo_2.10"),
                      publish = true)(as.json4s.Json)
```

## metadata

You can assign typed metadata as [attributes](https://bintray.com/docs/api/#_attributes) to packages and versions.
Bintray expects these to be of type `string`, `date`, `number`, `boolean` or `version`. Bintry exposes these types as
`Attr.String(stringVal)`, `Attr.Date(java.util.Date)`, `Attr.Number(intVal)`, `Attr.Boolean(boolVal)` and  `Attr(stringVal)` respectively.


Doug Tangren (softprops) 2013-2014
