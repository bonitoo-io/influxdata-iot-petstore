# InfluxDB 2.0 Java client Vaadin demo

[![Build Status](https://travis-ci.org/rhajek/influx-demo.svg?branch=master)](https://travis-ci.org/rhajek/influx-demo)

A project example for a [InfluxDB 2.0 java client]([https://github.com/bonitoo-io/influxdb-client-java]). 

It is a [Vaadin Flow](https://vaadin.com/flow) application that only requires a Servlet 3.1 container to run (no other JEE dependencies). 
The UI is built with Java only. 

## Prerequisites

The project can be imported into the IDE of your choice, with Java 8 installed, as a Maven project.

Demo uses [Vaadin Charts](https://vaadin.com/components/vaadin-charts) library for the metrics visualization. Vaadin Charts is
commercial library and license file is needed in order to compile a run the demo. Free trial license can be obtained from https://vaadin.com/trial.

InfluxDB 2.0 must be started on localhost on default port 9999.

You can simply run `./scripts/influxdb-restart.sh` to create and setup InfluxDB 2.0 server in docker. 

 You can start the new clean instance of InfluxDB manually using docker
by following command: 

```bash
docker run --rm --name my-influxdb2 --publish 9999:9999 quay.io/influxdb/influx:nightly
```

InfluxDB must be initialized before first usage. The user and the organization setup can be done using command line:

```bash

## onboarding
docker exec -it my-influxdb2 influx setup --username my-user --password my-password \
    --token my-token-123 --org my-org --bucket my-bucket --retention 48 --force

## show created orgId    
docker exec -it my-influxdb2 influx org find | grep my-org  | awk '{ print $1 }'

```
In the configuration file `src/main/resources/demo-config.properties` put generated orgId into `influxdb.orgId` property.

## Workflow

To compile the entire project, run "mvn install" in the parent project.

Other basic workflow steps:

- getting started
  - run `mvn jetty:run` in ui module
  - open http://localhost:8080/
- creating a production mode war
  - run `mvn package -Dvaadin.productionMode ` in the ui module or in the parent module
- running in production mode
  - run `mvn jetty:run -Dvaadin.productionMode` in ui module
  - open http://localhost:8080/
