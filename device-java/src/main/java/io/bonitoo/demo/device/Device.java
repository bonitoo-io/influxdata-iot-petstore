package io.bonitoo.demo.device;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
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
import org.influxdata.exceptions.UnauthorizedException;

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

    private String hubApiUrl;
    private String deviceId;
    private InfluxDBClient influxDBClient;
    private OkHttpClient client;
    private OnboardingResponse config;
    private WriteApi writeApi;
    private ScheduledExecutorService executor;

    private long interval;
    private TimeUnit intervalUnit;
    private boolean running = true;

    public Device() {
        this.deviceId = System.getProperty("deviceId", randomSerialNumber());
        this.hubApiUrl = System.getProperty("hubApiUrl");
        this.client = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
            .build();
        //write intervat in MS
        this.interval = Integer.parseInt(System.getProperty("interval", "30"));
        this.intervalUnit = TimeUnit.SECONDS;

        this.config = loadConfig();
        if (this.config != null) {
            setupDevice(config);
        }

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
               DeviceDiscovery.listAllMulticastInterfaces()
                .forEach(anInterface -> new DeviceDiscovery(this, anInterface).start());
        }
    }


    private OnboardingResponse loadConfig() {
        File file;
        try {
            file = new File(getConfigFileName());
        } catch (Exception e) {
            log.error("Invalid config location: " + getConfigFileName(), e.getLocalizedMessage());
            return null;
        }
        OnboardingResponse conf = null;
        if (file.canRead()) {
            try (
                FileReader fr = new FileReader(file)
            ) {
                Gson gson = new GsonBuilder().create();
                conf = gson.fromJson(fr, OnboardingResponse.class);
                this.deviceId = conf.deviceId;

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

        if (hubApiUrl == null && config == null) {
            log.info("Searching for hub...");
            return;
        }

        Request request = new Request.Builder().url(hubApiUrl + "/register/" + getDeviceId()).build();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            assert response.body() != null;
            String jsonBody = response.body().string();
            log.debug("Registration response status: " + code + " : " + jsonBody);
            switch (code) {
                case 200: {
                    Gson gson = new GsonBuilder().create();
                    boolean setupDeviceSucess = setupDevice(gson.fromJson(jsonBody, OnboardingResponse.class));
                    if (setupDeviceSucess) {
                        saveConfig();
                    }
                    return;
                }
                case 201: {
                    log.info("Waiting for device authorization... " + response.message());
                    return;
                }
                case 204: {
                    log.info("Device is already registered in Hub.");
                    return;
                }
                default: {
                    log.error("Device registration error " + code + ". " + response.message());
                }
            }
        } catch (Exception e) {
            log.error("Register device failed: " + e.getLocalizedMessage());
        }
    }


    private boolean setupDevice(OnboardingResponse config) {

        influxDBClient = InfluxDBClientFactory.create(config.url, config.authToken.toCharArray());
        Check health = influxDBClient.health();
        this.config = config;

        writeApi = influxDBClient.getWriteApi();

        writeApi.listenEvents(WriteSuccessEvent.class, (
            event) -> log.info("Write success. " + event.getLineProtocol()));

        writeApi.listenEvents(WriteErrorEvent.class,
            (event) -> {
                log.info("Write error " + event.getThrowable().getLocalizedMessage());

                Throwable e = event.getThrowable();
                //unauthorized attempt to write data -> reset device configuration
                if (e instanceof UnauthorizedException) {
                    this.config = null;

                    if (new File(getConfigFileName()).delete()) {
                        log.info("Config was reset.");
                    }
                }

            });
        writeApi.listenEvents(WriteRetriableErrorEvent.class,
            (event) -> log.info("Retryable Write error " + event.getThrowable().getLocalizedMessage()));

        return health.getStatus().equals(Check.StatusEnum.PASS);

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
        Point p = Point.measurement(getMeasurmentName());
        p.time(Instant.now(), WritePrecision.S);
        p.addTag("device_id", getDeviceId());
        String location = getLocation();
        if (location != null) {
            p.addTag("location", location);
        }
        p.addField("temperature", random(10, 40));
        p.addField("humidity", random(0, 100));
        p.addField("pressure", random(900, 1000));

        return Collections.singletonList(p);
    }

    public String getLocation() {
        return System.getProperty("location");
    }

    public String getMeasurmentName() {
        return System.getProperty("measurement", "air");
    }

    void setHubApiUrl(final String url) {
        this.hubApiUrl = url;
    }

    public String getHubApiUrl() {
        return hubApiUrl;
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

    boolean isRegistered() {
        return config != null;
    }


    public String getDeviceId() {
        return this.deviceId;
    }

    private String getConfigFileName() {
        return System.getProperty("config", "./iot-device.conf");
    }

    private void saveConfig() {

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

}
