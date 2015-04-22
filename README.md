Cloudfier
=========

Basic build: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-build)](https://textuml.ci.cloudbees.com/job/cloudfier-build/)

Tests: [![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=cloudfier-tests)](https://textuml.ci.cloudbees.com/job/cloudfier-tests/)

This repository contains the code for [Cloudfier](http://cloudfier.com), a web-based environment for modeling with support for editing, testing, deploying and generating business applications based on executable models.

Subsystems:
- kirra-mdd provides a [Kirra](abstratt/kirra) compatible view over UML models.
- codegen provides a code generation subsystem
- runtime provides a model interpreter subsystem (both UML-centric and Kirra-centric)
- saas provides the components required to support the Cloudfier SaaS environment: Orion integration, product packaging etc.
