package io.bonitoo.influxdemo.test;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import org.influxdata.query.dsl.functions.restriction.Restrictions;

import io.bonitoo.influxdemo.entities.Sensor;
import io.bonitoo.influxdemo.services.InfluxDBService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class InfluxDBServiceTest {
    Logger log = Logger.getLogger(InfluxDBServiceTest.class.getName());

    @Test
    public void testInfluxDBService() {

        InfluxDBService influxDBService = InfluxDBService.getInstance(false);
        QueryApi queryApi = influxDBService.getPlatformClient().getQueryApi();

        Flux query = Flux.from(influxDBService.getBucket())
            .filter(Restrictions.measurement().equal("sensor"))
            .range(-1L, ChronoUnit.HOURS);

        List<FluxTable> query1 = queryApi.query(query.toString(), influxDBService.getOrgId());

        Assertions.assertThat(query1).isNotNull();
    }

    @Test
    public void testWritePoint() {

        InfluxDBService influxDBService = InfluxDBService.getInstance(false);
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
    public void testQuery() {
        InfluxDBService influxDBService = InfluxDBService.getInstance();

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


}
