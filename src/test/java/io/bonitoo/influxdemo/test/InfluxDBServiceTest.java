package io.bonitoo.influxdemo.test;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.influxdata.LogLevel;
import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.QueryApi;
import org.influxdata.query.FluxTable;
import org.influxdata.query.dsl.Flux;
import org.influxdata.query.dsl.functions.properties.TimeInterval;
import org.influxdata.query.dsl.functions.restriction.Restrictions;
import org.influxdata.spring.influx.InfluxDB2Properties;

import io.bonitoo.influxdemo.services.InfluxDBService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = InfluxDBService.class)
@EnableConfigurationProperties
@EnableAutoConfiguration
public class InfluxDBServiceTest {
    private static Logger log = Logger.getLogger(InfluxDBServiceTest.class.getName());

    @Autowired
    InfluxDBClient influxDBClient;

    @Autowired
    InfluxDB2Properties properties;

    @Before
    public void enableLogging() {
        influxDBClient.setLogLevel(LogLevel.BODY);
    }

    @Test
    public void testInfluxDBService() {

        QueryApi queryApi = influxDBClient.getQueryApi();

        Flux query = Flux.from(properties.getBucket())
            .range(-10l, ChronoUnit.MINUTES)
            .filter(Restrictions.measurement().equal("sensor"));


        List<FluxTable> query1 = queryApi.query(query.toString());

        Assertions.assertThat(query1).isNotNull();
    }


    @Test
    public void testQueryParametrized() {

        influxDBClient.setLogLevel(LogLevel.BODY);
        QueryApi queryApi = influxDBClient.getQueryApi();

        Map<String, Object> properties = new HashMap<>();
        properties.put("every", new TimeInterval(15L, ChronoUnit.MINUTES));
        properties.put("period", new TimeInterval(20L, ChronoUnit.SECONDS));


        Flux flux = Flux.from(this.properties.getBucket())
            .range(-5l, ChronoUnit.MINUTES)
            .filter(Restrictions.measurement().equal("sensor"))
            .filter(Restrictions.field().equal("temperature"))
            .filter(Restrictions.column("device_id").equal("sensor3"))
            .limit().withN(10);
        //.withPropertyValue("offset", 1l)

        System.out.println(flux);
        List<FluxTable> query = queryApi.query(flux.toString(properties));

    }
    private static final long ASSYNC_TIMEOUT = 10;

    public QueryApi getQueryApi() {
        return influxDBClient.getQueryApi();
    }

}
