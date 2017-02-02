# OCPI endpoints [![Build Status](https://travis-ci.org/NewMotion/ocpi-endpoints.png?branch=master)](https://travis-ci.org/NewMotion/ocpi-endpoints)


The New Motion implementation of common endpoints of the OCPI protocol.

See http://github.com/ocpi/ocpi

# Version
The version of ocpi the library implements is hardcoded in `ocpi.ourVersion`

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
