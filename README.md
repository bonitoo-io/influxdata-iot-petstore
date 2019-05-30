# InfluxDB 2.0 Java client Vaadin demo

[![Build Status](https://travis-ci.org/rhajek/influx-demo.svg?branch=master)](https://travis-ci.org/rhajek/influx-demo)


A project example for a [InfluxDB 2.0 java client]([https://github.com/bonitoo-io/influxdb-client-java]). 

UI is implemented in [Vaadin Flow](https://vaadin.com/flow). Backend is integrated using [Spring Boot](https://spring.io/projects/spring-boot). 
The UI is built with Java only. [Micrometer.io](https://micrometer.io/) framework is used for monitoring. Metrics from application are exposed in Prometheus format.

#### Architecture

![Architecture](doc/architecture.png)

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

InfluxDB must be initialized before the first usage. The user and the organization setup can be done using command line:

```bash

## onboarding
docker exec -it my-influxdb2 influx setup --username my-user --password my-password \
    --token my-token-123 --org my-org --bucket my-bucket --retention 48 --force

## show created orgId    
docker exec -it my-influxdb2 influx org find | grep my-org  | awk '{ print $1 }'

```
In the configuration file `src/main/resources/application.properties` put generated orgId into `influxdb.orgId` property.

## Workflow

To compile the entire project, run "mvn install" in the project directory.

Other basic workflow steps:

- getting started
  - run `mvn jetty:run` in ui module
  - open http://localhost:8080/
- creating a production mode war
  - run `mvn package -Dvaadin.productionMode ` 
- running in production mode
  - run `mvn jetty:run -Dvaadin.productionMode` 
  - open http://localhost:8080/

- running using spring-boot
  - run `mvn spring-boot:run -Dvaadin.productionMode` 
  - open http://localhost:8080/
  
  
## Prometheus metrics
-  http://localhost:8080/actuator/prometheus
 
## Screenshot example
![Example](doc/browse.png)

## InfluxDB client Spring integration

IoT PetStore demo shows also how to integrate InfluxDB 2.0 client libraries into Spring Boot project. 

###  Micrometer.io integration
Micrometer is a great metrics instrumentation library for JVM-based applications. Starting with Spring Boot 2.0, Micrometer is the
default instrumentation library powering the delivery of application metrics. Using micrometer is possible to expose both
application and system level metrics. 

Spring boot [autoconfigures](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-metrics-meter) most commonly used metrics for you, including:

* JVM, report utilization of:
    * Various memory and buffer pools
    * Statistics related to garbage collection
    * Thread utilization
    * Number of classes loaded/unloaded
* CPU usage
* Spring MVC and WebFlux request latencies
* RestTemplate latencies
* Cache utilization
* Datasource utilization, including connection pool metrics
* File descriptor usage
* Logback: record the number of events logged to Logback at each level
* System Uptime 
* Tomcat usage, user sessions 

Micrometer can use InfluxDB 2.0 as a target database for pushing metrics data.

Implementation ```io.micrometer.influx2.Influx2MeterRegistry``` is located in Bonitoo fork of Micrometer GitHub repository, 
[micrometer-registry-influx2](https://github.com/bonitoo-io/micrometer/tree/influx2-registry/implementations/micrometer-registry-influx2).

All configuration of micrometer exporter is in ```application.properties```.  

```properties
management.metrics.export.influx2.enabled=true
management.metrics.export.influx2.uri=http://localhost:9999/api/v2
management.metrics.export.influx2.org=my-org
management.metrics.export.influx2.bucket=micrometer-bucket
management.metrics.export.influx2.token=my-token-123
management.metrics.export.influx2.step=10s
```

### Configuration of InfluxDB 2.0 client library in Spring 

InfluxDB 2.0 client library can be seamlessly used in Spring. Client dependency injection and is possible to auto-configure using 
standard spring application properties. If the client is configured, actuator for InfluxDB health service is automatically enabled by default.

To enable InfluxDB 2.0 client in your Spring application, you need only to add maven dependency

```xml
<dependency>
  <groupId>org.influxdata</groupId>
  <artifactId>influxdb-spring</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
``` 

and configure client following client properties:

```properties
spring.influx2.url=http://localhost:9999
spring.influx2.org=03d802a38739a000
spring.influx2.bucket=my-bucket
spring.influx2.token=my-token-123
spring.influx2.username=my-user
```
After that you can  use ```@Autowired``` annotation and inject client ```org.influxdata.client.InfluxDBClient``` in your Spring service and component.

Source code and detail description of Spring and InfluxDB2.0 integration is located in    
[https://github.com/bonitoo-io/influxdb-client-java/tree/master/spring](https://github.com/bonitoo-io/influxdb-client-java/tree/master/spring) repository.
