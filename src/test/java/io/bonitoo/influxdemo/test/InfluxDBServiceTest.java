package io.bonitoo.influxdemo.test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import org.influxdata.LogLevel;
import org.influxdata.client.QueryApi;
import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.events.EventListener;
import org.influxdata.client.write.events.ListenerRegistration;
import org.influxdata.client.write.events.WriteSuccessEvent;
import org.influxdata.query.FluxTable;
import org.influxdata.query.dsl.Flux;
import org.influxdata.query.dsl.functions.properties.TimeInterval;
import org.influxdata.query.dsl.functions.restriction.Restrictions;

import io.bonitoo.influxdemo.entities.Sensor;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest (classes = InfluxDBService.class)
@EnableConfigurationProperties

public class InfluxDBServiceTest {
    private static Logger log = Logger.getLogger(InfluxDBServiceTest.class.getName());

    @Autowired
    InfluxDBService influxDBService;

    @Before
    public  void enableLogging () {
       influxDBService.getPlatformClient().setLogLevel(LogLevel.BODY);
    }

    @Test
    public void testInfluxDBService() {

        QueryApi queryApi = influxDBService.getPlatformClient().getQueryApi();

        Flux query = Flux.from(influxDBService.getBucket())
            .range(-10l, ChronoUnit.MINUTES)
            .filter(Restrictions.measurement().equal("sensor"));


        List<FluxTable> query1 = queryApi.query(query.toString(), influxDBService.getOrgId());

        Assertions.assertThat(query1).isNotNull();
    }

    @Test
    public void testWritePoint() {

        influxDBService.getPlatformClient().setLogLevel(LogLevel.BODY);
        WriteApi writeApi = influxDBService.getPlatformClient().getWriteApi();

        Sensor sensor = new Sensor();
        sensor.setTemperature(10);
        sensor.setHumidity(90);
        sensor.setBatteryCapacity(100);
        sensor.setLocation("Prague");
        sensor.setSid("test-sid");

        WriteEventListener<WriteSuccessEvent> listener = new WriteEventListener<>();

        ListenerRegistration listenerRegistration = writeApi.listenEvents(WriteSuccessEvent.class, listener);

        writeApi.writeMeasurement(influxDBService.getBucket(), influxDBService.getOrgId(), WritePrecision.MS, sensor);

        waitToCallback(listener.getCountDownLatch());
        listener.getValue().logEvent();
        log.info("test logging");

        listenerRegistration.dispose();
    }

    @Test
    public void testQueryParametrized() {

        influxDBService.getPlatformClient().setLogLevel(LogLevel.BODY);
        QueryApi queryApi = influxDBService.getPlatformClient().getQueryApi();

        Map<String, Object> properties = new HashMap<>();
        properties.put("every", new TimeInterval(15L, ChronoUnit.MINUTES));
        properties.put("period", new TimeInterval(20L, ChronoUnit.SECONDS));


        Flux flux = Flux.from(influxDBService.getBucket())
            .range(-5l, ChronoUnit.MINUTES)
            .filter(Restrictions.measurement().equal("sensor"))
            .filter(Restrictions.field().equal("temperature"))
            .filter(Restrictions.column("sid").equal("sensor3"))
            .limit().withN(10);
        //.withPropertyValue("offset", 1l)
        ;

        System.out.println(flux);
        List<FluxTable> query = queryApi.query(flux.toString(properties), influxDBService.getOrgId());

    }

    @Test
    public void testQuery() {

        influxDBService.getPlatformClient().setLogLevel(LogLevel.BODY);
        QueryApi queryApi = influxDBService.getPlatformClient().getQueryApi();
        WriteApi writeApi = influxDBService.getPlatformClient().getWriteApi();


        Sensor sensor = new Sensor();
        sensor.setTemperature(10);
        sensor.setHumidity(90);
        sensor.setBatteryCapacity(100);
        sensor.setLocation("Prague");
        sensor.setSid("test-sid");

        WriteEventListener<WriteSuccessEvent> listener = new WriteEventListener<>();
        ListenerRegistration listenerRegistration = writeApi.listenEvents(WriteSuccessEvent.class, listener);
        writeApi.writeMeasurement(influxDBService.getBucket(), influxDBService.getOrgId(), WritePrecision.MS, sensor);
        waitToCallback(listener.getCountDownLatch());
        listenerRegistration.dispose();

        Flux query = Flux.from(influxDBService.getBucket())
            .filter(Restrictions.measurement().equal("sensor"))
            .filter(Restrictions.tag("sid").equal("test-sid"))
            .filter(Restrictions.tag("location").equal("Prague"))
            .range(-1L, ChronoUnit.HOURS).last();

        List<FluxTable> query1 = queryApi.query(query.toString(), influxDBService.getOrgId());

        Assertions.assertThat(query1).isNotNull();
        Assertions.assertThat(query1.size()).isGreaterThan(0);
    }

    private static final long ASSYNC_TIMEOUT = 10;

    static void waitToCallback(@Nonnull final CountDownLatch countDownLatch) {
        try {
            org.assertj.core.api.Assertions
                .assertThat(countDownLatch.await(ASSYNC_TIMEOUT, TimeUnit.SECONDS))
                .overridingErrorMessage(
                    "The countDown wasn't counted to zero. Before elapsed: %s seconds.", ASSYNC_TIMEOUT)
                .isTrue();
        } catch (InterruptedException e) {
            org.assertj.core.api.Assertions.fail("Unexpected exception", e);
        }
    }


    class WriteEventListener<T> implements EventListener<T> {

        private CountDownLatch countDownLatch;
        private List<T> values = new ArrayList<>();

        WriteEventListener() {
            countDownLatch = new CountDownLatch(1);
        }

        public CountDownLatch getCountDownLatch() {
            return countDownLatch;
        }

        @Override
        public void onEvent(@Nonnull final T value) {

            org.assertj.core.api.Assertions.assertThat(value).isNotNull();

            values.add(value);

            countDownLatch.countDown();
        }

        T getValue() {
            return values.get(0);
        }

        T popValue() {
            T value = values.get(0);
            values.remove(0);
            return value;
        }
    }


    public QueryApi getQueryApi() {
        return influxDBService.getPlatformClient().getQueryApi();
    }

}
