package io.bonitoo.demo.device;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.InfluxDBClientFactory;
import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.WritePrecision;
import org.influxdata.client.write.Point;
import org.influxdata.client.write.events.WriteErrorEvent;
import org.influxdata.client.write.events.WriteRetriableErrorEvent;
import org.influxdata.client.write.events.WriteSuccessEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import static java.lang.Thread.sleep;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * This class demonstrates IoT Device, that registers to IoT Hub and sends data in regular interval.
 */
public class Device {

    private static Logger log = Logger.getLogger(Device.class.getName());

    //iot hub hubApiUrl
    private String hubApiUrl;
    private String deviceNumber; //like "G3D5-34FG-PRE1-S3AP";
    private OkHttpClient client;
    private OnboardingResponse config;
    private InfluxDBClient influxDBClient;
    private WriteApi writeApi;
    private ScheduledExecutorService executor;

    private long interval;
    private TimeUnit intervalUnit;

    Device() {
        this.deviceNumber = SerialNumberHelper.randomSerialNumber();
        this.hubApiUrl = System.getProperty("hubApiUrl", "http://localhost:8080/api");
        this.client = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
            .build();
        //write intervat in MS
        this.interval = 1000;
        this.intervalUnit = TimeUnit.MILLISECONDS;

        startScheduler();
    }

    public static void main(String[] args) throws InterruptedException {
        Device device = new Device();

        while (!device.executor.isTerminated()) sleep(1000L);
    }

    private void startScheduler() {
        executor = Executors.newScheduledThreadPool(0);
        executor.scheduleAtFixedRate(this::loop, 0, interval, intervalUnit);
    }

    /**
     * Device registration and authorization
     */
    private void register() {

        Request request = new Request.Builder().url(hubApiUrl + "/register/" + deviceNumber).build();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            assert response.body() != null;
            String jsonBody = response.body().string();
            log.log(Level.FINE, "Registration response status: " + code + " : " + jsonBody);
            switch (code) {
                case 200: {
                    setupDevice(jsonBody);
                    return;
                }
                case 201: {
                    log.info("Waiting for device authorization.. " + response.message());
                    return;
                }
                default: {
                    log.info("Device registration error " + code + ". " + response.message());
                }
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Register device failed: " + e.getLocalizedMessage());
        }
    }

    private void setupDevice(final String jsonConfig) {

        Gson gson = new GsonBuilder().create();
        // parse json string to object
        this.config = gson.fromJson(jsonConfig, OnboardingResponse.class);

        influxDBClient = InfluxDBClientFactory.create(config.url, config.authToken.toCharArray());
        writeApi = influxDBClient.getWriteApi();
        writeApi.listenEvents(WriteSuccessEvent.class, (
            event) -> log.info("Write success. " + event.getLineProtocol()));
        writeApi.listenEvents(WriteErrorEvent.class,
            (event) -> {
                log.info("Write error " + event.getThrowable().getLocalizedMessage());
                config = null;
            });
        writeApi.listenEvents(WriteRetriableErrorEvent.class,
            (event) -> log.info("Retryable Write error " + event.getThrowable().getLocalizedMessage()));
    }

    private void loop() {
        //if device is not registered -> register
        if (config == null) {
            register();
            //write data
        } else {
            writeApi.writePoints(config.bucket, config.orgId, getMetrics());
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private List<Point> getMetrics() {
        Point p = Point.measurement("sensor");
        p.time(Instant.now(), WritePrecision.S);
        p.addTag("sid", deviceNumber);
        p.addField("temperature", random(10, 40));
        p.addField("humidity", random(0, 100));

        return Collections.singletonList(p);
    }

    //registration api response
    @SuppressWarnings("unused")
    private class OnboardingResponse {
        private String deviceId;
        private String url;
        private String orgId;
        private String authToken;
        private String bucket;
    }

    //helper random generator
    private static double random(double rangeMin, double rangeMax) {
        Random r = new Random();
        return rangeMin + (rangeMax - rangeMin) * r.nextDouble();
    }

}
