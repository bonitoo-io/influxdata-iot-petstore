# InfluxDB 2.0 Java client Vaadin demo

A project example for a [InfluxDB 2.0 java client]([https://github.com/bonitoo-io/influxdb-client-java]). 

It is a Vaadin application that only requires a Servlet 3.1 container to run (no other JEE dependencies). 
The UI is built with Java only.

## Prerequisites

The project can be imported into the IDE of your choice, with Java 8 installed, as a Maven project.

InfluxDB 2.0 must be started on default 9999. You can start the new clean instance of InfluxDB using docker
be following command: 

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
