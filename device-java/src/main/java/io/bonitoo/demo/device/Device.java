package io.bonitoo.demo.device;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.influxdata.client.InfluxDBClient;
import org.influxdata.client.InfluxDBClientFactory;
import org.influxdata.client.WriteApi;
import org.influxdata.client.domain.Check;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class demonstrates IoT Device, that registers to IoT Hub and sends data in regular interval.
 */
public class Device {

    private static Logger log = LoggerFactory.getLogger(Device.class);

    //iot hub hubApiUrl
    private String hubApiUrl;
    private String deviceNumber;
    private OkHttpClient client;
    private OnboardingResponse config;
    private InfluxDBClient influxDBClient;
    private WriteApi writeApi;
    private ScheduledExecutorService executor;

    private long interval;
    private TimeUnit intervalUnit;
    private boolean running = true;

    public Device() {
        this.deviceNumber = System.getProperty("deviceNumber", randomSerialNumber());
        this.hubApiUrl = System.getProperty("hubApiUrl");
        this.client = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
            .build();
        //write intervat in MS
        this.interval = 5000;
        this.intervalUnit = TimeUnit.MILLISECONDS;
        startScheduler();
    }

    public static void main(String[] args) throws InterruptedException {
        Device device = new Device();
        do {
            Thread.sleep(10000);
        } while (!device.running);
    }

    private void startScheduler() {
        //run executor as daemon thread
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::loop, 0, interval, intervalUnit);
        if (hubApiUrl == null) {
            try {
                DeviceDiscovery deviceDiscovery = new DeviceDiscovery(this);
                deviceDiscovery.start();
            } catch (IOException e) {
                log.info("device discovery error: " + e.toString());
            }
        }
    }


    private String getConfigFileName() {
        return System.getProperty("config", "./iot-device.conf");
    }

    private OnboardingResponse loadConfig() {
        File file = null;
        try {
            file = new File(getConfigFileName());
        } catch (Exception e) {
            log.error("Invalid config location: "+getConfigFileName(), e.getLocalizedMessage());
            return null;
        }
        OnboardingResponse conf = null;
        if (file.canRead()) {
            try (
                FileReader fr = new FileReader(file)
            ) {
                Gson gson = new GsonBuilder().create();
                conf = gson.fromJson(fr, OnboardingResponse.class);
                this.deviceNumber = conf.deviceId;

            } catch (IOException e) {
                log.error("Error while loading config", e);
            }
        }
        return conf;
    }

    /**
     * Device registration and authorization
     */
    private void register() {

        OnboardingResponse config = loadConfig();

        if (config != null) {
            boolean success = setupDevice(config);
            if (success) {
                return;
            }
        }

        if (hubApiUrl == null) {
            log.info("Searching for hub...");
            return;
        }

        Request request = new Request.Builder().url(hubApiUrl + "/register/" + getDeviceNumber()).build();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            assert response.body() != null;
            String jsonBody = response.body().string();
            log.debug("Registration response status: " + code + " : " + jsonBody);
            switch (code) {
                case 200: {
                    setupDevice(jsonBody);
                    return;
                }
                case 201: {
                    log.info("Waiting for device authorization... " + response.message());
                    return;
                }
                default: {
                    log.info("Device registration error " + code + ". " + response.message());
                }
            }
        } catch (Exception e) {
            log.error("Register device failed: " + e.getLocalizedMessage());
        }
    }

    private void saveConfig(OnboardingResponse config) {

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            log.info("Saving configuration to: " + getConfigFileName());
            FileWriter writer = new FileWriter(getConfigFileName());
            gson.toJson(config, writer);
            writer.flush();
            writer.close();

        } catch (IOException e) {
            log.error("Unable to save configuration file.", e);
        }

    }

    boolean setupDevice(OnboardingResponse config) {
        influxDBClient = InfluxDBClientFactory.create(config.url, config.authToken.toCharArray());

        Check health = influxDBClient.health();

        this.config = config;

        writeApi = influxDBClient.getWriteApi();

        writeApi.listenEvents(WriteSuccessEvent.class, (
            event) -> log.info("Write success. " + event.getLineProtocol()));
        writeApi.listenEvents(WriteErrorEvent.class,
            (event) -> {
                log.info("Write error " + event.getThrowable().getLocalizedMessage());
                this.config = null;
                new File(getConfigFileName()).delete();
            });
        writeApi.listenEvents(WriteRetriableErrorEvent.class,
            (event) -> log.info("Retryable Write error " + event.getThrowable().getLocalizedMessage()));

        if (health.getStatus().equals(Check.StatusEnum.PASS)) {
            saveConfig(config);
            return true;
        }

        return false;
    }

    boolean setupDevice(final String jsonConfig) {
        Gson gson = new GsonBuilder().create();
        return setupDevice(gson.fromJson(jsonConfig, OnboardingResponse.class));
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

    void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
        running = false;
    }

    public List<Point> getMetrics() {
        Point p = Point.measurement("sensor");
        p.time(Instant.now(), WritePrecision.S);
        p.addTag("sid", getDeviceNumber());
        p.addField("temperature", random(10, 40));
        p.addField("humidity", random(0, 100));
        p.addField("pressure", random(900, 1000));

        return Collections.singletonList(p);
    }

    void setHubApiUrl(final String url) {
        this.hubApiUrl = url;
    }

    boolean isRegistered() {
        return config != null;
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


    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    /**
     * Generates random device number.
     * <p>
     * return random string in "%s-%s-%s-%s" format
     **/
    private String randomSerialNumber() {
        return String.format("%s-%s-%s-%s", randomAlphaNumeric(4), randomAlphaNumeric(4), randomAlphaNumeric(4), randomAlphaNumeric(4));
    }

    public boolean isRunning() {
        return running;
    }

    public String getDeviceNumber() {
        return this.deviceNumber;
    }

}
