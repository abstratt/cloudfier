Cloudfier
=========

Basic build: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-build)](https://textuml.ci.cloudbees.com/job/cloudfier-build/)

Tests: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-tests)](https://textuml.ci.cloudbees.com/job/cloudfier-tests/)

This repository contains the code for [Cloudfier](http://cloudfier.com), a web-based environment for modeling with support for editing, testing, deploying and generating business applications based on executable models.

> ***If you just want to learn about Cloudfier, and how to use to develop model-based applications, stop reading this and instead head to the [Cloudfier documentation](http://doc.cloudfier.com).***

### Code organization 

The components that make up Cloudfier are divided among subsystems:
- [kirra-mdd](kirra-mdd/) provides a [Kirra](http://github.com/abstratt/kirra/) compatible view over UML models.
- [codegen](codegen) provides a code generation subsystem, including some code generators such as [Expert4JEE](codegen/com.abstratt.mdd.target.jee/).
- [runtime](runtime/) provides a model interpreter subsystem (containing components that are either UML-centric or Kirra-centric)
- [saas](saas/) provides the components required to support the Cloudfier SaaS environment: Orion integration, product packaging etc.

Also, the TextUML Toolkit project, which has [its own repository](http://github.com/abstratt/textuml), provides a number of core components to Cloudfier: model repository management, front-end infrastructure and support for the primary notation (TextUML), and a number of model manipulation utilities.

## Developing Cloudfier

### Requirements

  * Java 8 or later 
  * Maven 3.3.x 
  * A git client
  * Postgres 9

You You also need a database named "cloudfier" accessible (can create/drop schemas) to a user named "cloudfier" with no password. 

### Building

You can build and run the tests the usual way:

```
mvn clean install
```

### Running

> This is a work-in-progress. The instructions won't allow you yet to run a fully functional Cloudfier instance (steps for configuring the development environment integration are not included yet), but you can use this server via the cloudfier-maven-plugin (details below).

After building, you can run the tooling/runtime back-end this way (on Linux - the build is currently not generating packages for other platforms, help wanted here):
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
	External: http://localhost/services/
	Internal: http://localhost/services/
osgi> 
```

Your local Cloudfier server is now up and running.

### Using your local Cloudfier server via the cloudfier-maven-plugin

You can use those Cloudfier features exposed via the cloudfier-maven-plugin (starting with version 0.12.0 of the plugin). In order to do that, once the server is up and running, follow the instructions in the [cloudfier-maven-plugin project](http://github.com/abstratt/cloudfier-maven-plugin), and make sure you always specify the -Dkirra.uri property pointing to your local instance, for example:

mvn com.abstratt:cloudfier-maven-plugin:publish -Dkirra.uri=http://localhost:8081/services

### Running the Orion integration

In order to run a local development environment, you need to install Orion. 

TBD


###  Continuous builds

Continuous builds run on [Jenkins at
CloudBees](http://textuml.ci.cloudbees.com/). Artifact repository is also
available at [CloudBees Forge](http://repository-
textuml.forge.cloudbees.com/snapshot/).

### Developing Cloudfier in the IDE

####  Requirements

  * Eclipse Luna or newer([download](http://www.eclipse.org/downloads/)) 
  * M2E (Maven Integration for Eclipse) 1.5.x - Maven Integration for Eclipse (with Tycho support) 
  * Xtext SDK 2.8.x (use their own [update site](http://download.eclipse.org/modeling/tmf/xtext/updates/releases/))

You can easily obtain M2E, Xtext and EGit from the Eclipse Marketplace. If you
don't have the Marketplace client installed (check Help &gt; Eclipse
Marketplace), install it from here:
[https://www.eclipse.org/mpc/](https://www.eclipse.org/mpc/)

####  Importing the source code into Eclipse

Use the M2E import wizard (Import... &gt; Maven &gt; Existing Maven Projects) and
point it to the root directory for the cloudfier git workspace. It should find all Cloudfier modules
inside that directory.

After the sources are imported, you should choose the target definition file
cloudfier/kirra-dependencies/kirra-dependencies.target as your target
platform (Window &gt; Preferences &gt; Plug-n Development &gt; Target Platform &gt;
Kirra Dependencies Target, or kirra-ide-dependencies.target if you also have the TextUML Toolkit source code loaded into Eclipse). Loading the target platform may take a long time (several minutes, much of it apparently stuck at 0%), so be patient and wait until the "Load Target Platform" job completes. Once it completes, you should have no error markers in your workspace.

####  Running Cloudfier as an Eclipse application

1. Open the kirra.product product definition file (find it with Ctrl-Shift-R)
2. Launch the product from the Testing section on the Overview page. For more information, see [this](http://help.eclipse.org/luna/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fproduct_editor%2Flaunching.htm)
3. From there on, you can use the launch configuration that was created during the previous step for relaunching Cloudfier


### Licensing

The code in this repository is licensed under one of the following licenses: EPL or AGPL. Look for the closest [LICENSE file](https://github.com/abstratt/cloudfier/search?q=filename%3ALICENSE) for more details. 

## Related repositories

This is just for your information about where other related pieces live. You should NOT need to load any of the following respositories to develop Cloudfier.

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

