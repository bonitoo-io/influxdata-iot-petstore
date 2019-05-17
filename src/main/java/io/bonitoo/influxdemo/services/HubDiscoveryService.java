package io.bonitoo.influxdemo.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
@ConfigurationProperties(prefix = "petstore")
@Configuration
public class HubDiscoveryService {

    private static Logger log = LoggerFactory.getLogger(HubDiscoveryService.class);

    private String hubUrl;

    private String multicastPort;
    private String multicastAddress;

    @Autowired
    private ApplicationContext applicationContext;


    public String getHubApi() {

        if (hubUrl != null) {
            return hubUrl;
        } else {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                String port = applicationContext.getBean(Environment.class).getProperty("server.port");
                return "http://" + host + ":" + port + "/api";

            } catch (UnknownHostException ignored) {
            }
        }
        throw new IllegalStateException("Unable to detect hubApi");
    }


    List<InetAddress> listAllBroadcastAddresses() {
        List<InetAddress> broadcastList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                networkInterface.getInterfaceAddresses().stream()
                    .map(InterfaceAddress::getBroadcast)
                    .filter(Objects::nonNull)
                    .forEach(broadcastList::add);
            }
        } catch (SocketException e) {
            log.error(e.getMessage(), e);
        }
        return broadcastList;
    }


    @Scheduled(cron = "${petstore.multicastCron:-}")
    public void broadcastDiscovery() {

        DatagramSocket socket = null;

        List<InetAddress> inetAddresses = listAllBroadcastAddresses();

        for (InetAddress address : inetAddresses) {
            try {
                socket = new DatagramSocket();
                String broadcastMessage = "[petstore.hubUrl=" + getHubApi() + "]";

                socket.setBroadcast(true);
                byte[] buffer = broadcastMessage.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, Integer.parseInt(multicastPort));
                socket.send(packet);

                log.info("Sending discovery broadcast: " + broadcastMessage + " address: " + address.getHostAddress() + " ");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally {
                Objects.requireNonNull(socket).close();
            }

        }

    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(final String multicastPort) {
        this.multicastPort = multicastPort;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(final String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }
}
