Cloudfier
=========

Basic build: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-build)](https://textuml.ci.cloudbees.com/job/cloudfier-build/)

Tests: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-tests)](https://textuml.ci.cloudbees.com/job/cloudfier-tests/)

This repository contains the code for [Cloudfier](http://cloudfier.com), a web-based environment for modeling with support for editing, testing, deploying and generating business applications based on executable models.

Subsystems:
- kirra-mdd provides a [Kirra](http://github.com/abstratt/kirra/) compatible view over UML models.
- codegen provides a code generation subsystem
- runtime provides a model interpreter subsystem (both UML-centric and Kirra-centric)
- saas provides the components required to support the Cloudfier SaaS environment: Orion integration, product packaging etc.

## Related repositories

### cloudfier-examples

https://github.com/abstratt/cloudfier-examples

Simple Cloudfier applications that help demonstrate and validate Cloudfier. You can clone that repo into your Cloudfier repository and play with Cloudfier's features.

### codegen-examples

https://github.com/abstratt/codegen-examples

Command-line (bash-only) tools for generating code for the target platforms supported in Cloudfier.

### cloudfier-maven-plugin

https://github.com/abstratt/cloudfier-maven-plugin

Plugin that exposes the functionality of a Cloudfier server to a Maven build. 

### TextUML

https://github.com/abstratt/textuml

The core model compilation functionality using TextUML as front-end notation.

## Building

You need to have Maven 3, Java 8 and Postgres 9 installed. You also need a database named "cloudfier" accessible to a user named "cloudfier" with no password. You can build and run the tests the usual way:

```
mvn clean install
```

## Running

> This is a work-in-progress. The instructions won't allow you yet to run a fully functional Cloudfier instance (steps for configuring the development environment integration are not included yet), but you can use this server via the cloudfier-maven-plugin (details below).

After building, you can run the tooling/runtime back-end this way (on Linux):
```
cd saas/com.abstratt.kirra.server.product/
find target -name kirra-server
```
which will show the target platforms available, for example, on a Linux box:

```
target/products/com.abstratt.kirra.server.product/linux/gtk/x86/kirra-server
target/products/com.abstratt.kirra.server.product/linux/gtk/x86_64/kirra-server
```

Change into the directory of choice, and run:

```
./kirra-server -data {path/to/workspace}
```

which will show:

```
!SESSION 2015-04-23 12:58:11.358 -----------------------------------------------
eclipse.buildId=unknown
java.version=1.8.0_31
java.vendor=Oracle Corporation
BootLoader constants: OS=linux, ARCH=x86, WS=gtk, NL=en_US
Command-line arguments:  -os linux -ws gtk -arch x86 -console -consolelog

!ENTRY com.abstratt.mdd.frontend.web 1 0 2015-04-23 12:58:12.095
!MESSAGE Started endpoint
	External: http://localhost/mdd/
	Internal: http://localhost/mdd/
osgi> 
```

Your Cloudfier server is now up and running.

### Using the Cloudfier server via the cloudfier-maven-plugin

You can use those Cloudfier features exposed via the cloudfier-maven-plugin (starting with version 0.12.0 of the plugin). In order to do that, once the server is up and running, follow the instructions in the [cloudfier-maven-plugin project](http://github.com/abstratt/cloudfier-maven-plugin), and make sure you always specify the -Dkirra.uri property pointing to your local instance, for example:

mvn com.abstratt:cloudfier-maven-plugin:publish -Dkirra.uri=http://localhost:8081/mdd

### Running the Orion integration

In order to run a local development environment, you need to install Orion. 

TBD

## Licensing

The code in this repository is licensed under one of the following licenses: EPL or AGPL. Look for the closest [LICENSE file](https://github.com/abstratt/cloudfier/search?q=filename%3ALICENSE) for more details. 

