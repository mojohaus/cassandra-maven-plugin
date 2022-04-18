# MojoHaus Cassandra Maven Plugin

This is the [cassandra-maven-plugin](http://www.mojohaus.org/cassandra-maven-plugin/).
 
[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.mojo/cassandra-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.mojo/cassandra-maven-plugin)
[![Build Status](https://travis-ci.org/mojohaus/cassandra-maven-plugin.svg?branch=master)](https://travis-ci.org/mojohaus/cassandra-maven-plugin)

## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
