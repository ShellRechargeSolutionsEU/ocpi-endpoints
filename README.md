# OCPI endpoints [![Build Status](https://travis-ci.org/NewMotion/ocpi-endpoints.png?branch=master)](https://travis-ci.org/NewMotion/ocpi-endpoints)


The New Motion implementation of common endpoints of the OCPI protocol.

See http://github.com/ocpi/ocpi

# Notions
To make clear when the parameters are about the application making use of the application or the party the application is
connecting to they are referred as: `our`/`us` or `theirs`/`them`

# Dependencies
To find out if there is any dependency to be updated you can run `sbt dependencyUpdates` to see what could be updated.
See https://github.com/rtimush/sbt-updates

# Usage

See the example app in the example folder.  You can run it with sbt by typing `project example` followed by `run`. 
Once running you can make version requests with

```
curl -X GET -H "Authorization: Token abc" "http://localhost:8080/example/versions"
```

# Serialization support

You can use either Circe (recommended) or Spray-Json for (de)serialization of JSON

## Circe

In build.sbt

```
libraryDependencies += "com.thenewmotion.ocpi" %% s"ocpi-msgs-circe" % "0.7.6"
```

Then in the route or client you want to use

```
import com.thenewmotion.ocpi.msgs.circe.v2_1.protocol._
```

## Spray Json

In build.sbt

```
libraryDependencies += "com.thenewmotion.ocpi" %% s"ocpi-msgs-spray-json" % "0.7.6"
```

Then in the route or client you want to use

```
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._
```
