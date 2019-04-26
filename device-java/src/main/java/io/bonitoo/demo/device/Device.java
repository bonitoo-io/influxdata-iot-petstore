package io.bonitoo.demo.device;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class Device {

    private long interval = 1000L;

    private class OnboardingResponse {
        private String deviceId;
        private String url;
        private String orgId;
        private String authToken;
        private String bucket;
    }

    private static Logger log = Logger.getLogger(Device.class.getName());

    //iot hub url
    private String url = "http://localhost:8080/api";

    private String deviceNumber = "G3D5-34FG-PRE1-S3AP";
    private OkHttpClient client;

    private OnboardingResponse config;
    private InfluxDBClient influxDBClient;
    private WriteApi writeApi;
    private ScheduledExecutorService executor;


    //
    public static void main(String[] args) {
        Device device = new Device();
        device.start();

        while (!device.executor.isTerminated()) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }

    }

    public void start() {
        executor = Executors.newScheduledThreadPool(0);
        executor.scheduleAtFixedRate(this::run, 0, interval, TimeUnit.MILLISECONDS);
    }

    public Device() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        client = new OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build();
    }

    /**
     * Returns true, when device is registered and authorized.
     */
    private boolean register() {
        Request request = new Request.Builder().url(url + "/register/" + deviceNumber).build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            assert response.body() != null;
            String body = response.body().string();
            System.out.println("Registration response status: " + code + " : " + body);

            switch (code) {
                case 202: {
                    log.info("");
                    return false;
                }

                case 201: {
                    log.info("Waiting for authorization");
                    return false;
                }
                case 200: {
                    setupDevice(body);
                    return true;
                }
            }
            return false;

        } catch (IOException e) {
            System.out.println("Register device failed: " + e.getLocalizedMessage());
            return false;
        }
    }

    private void setupDevice(final String jsonConfig) {
        Gson gson = new GsonBuilder().create();
        // parse json string to object
        this.config = gson.fromJson(jsonConfig, OnboardingResponse.class);

        influxDBClient = InfluxDBClientFactory.create(config.url, config.authToken.toCharArray());
        writeApi = influxDBClient.getWriteApi();
        writeApi.listenEvents(WriteSuccessEvent.class, (value) -> log.info("Write success."));
        writeApi.listenEvents(WriteErrorEvent.class, (value) -> {
            log.info("Write error " + value.getThrowable().getLocalizedMessage());
            config = null;

        });
        writeApi.listenEvents(WriteRetriableErrorEvent.class, (value) -> log.info("Retryable Write error " + value.getThrowable().getLocalizedMessage()));
    }

    private void run() {

        if (config == null || config.authToken == null) {
            boolean success = register();
            if (!success) {
                return;
            }
        }
        writeApi.writePoints(config.bucket, config.orgId, getMetrics());
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

    private static double random(double rangeMin, double rangeMax) {
        Random r = new Random();
        return rangeMin + (rangeMax - rangeMin) * r.nextDouble();
    }
}
