package io.bonitoo.influxdemo.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
    private int multicastPort;
    private String multicastAddress;

    @Autowired
    private Environment environment;

    public String getHubApi() {

        if (hubUrl != null) {
            return hubUrl;
        } else {
            try {
                String host = InetAddress.getLocalHost().getHostAddress();
                String port = environment.getProperty("server.port");
                return "http://" + host + ":" + port + "/api";

            } catch (UnknownHostException ignored) {
            }
        }
        throw new IllegalStateException("Unable to detect hubApi");
    }

    @Scheduled(cron = "${petstore.multicastCron:-}")
    public void multicastDiscovery() {
        List<NetworkInterface> networkInterfaces = listAllMulticastInterfaces();
        for (NetworkInterface networkInterface : networkInterfaces) {
            try {
                String message = "[petstore.hubUrl=" + getHubApi() + "]";
                sendMessage(multicastAddress, networkInterface.getName(), multicastPort, message);
                log.info("Sending multicast discovery: " + message + " address: " + multicastAddress + " iface: " + networkInterface.getName());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(final int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public void setMulticastAddress(final String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }

    public void sendMessage(String ip, String iface, int port, String message) throws IOException {

        DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.bind(null);

        List<NetworkInterface> networkInterfaces = listAllMulticastInterfaces();

        for (NetworkInterface networkInterface : networkInterfaces) {
            networkInterface = NetworkInterface.getByName(iface);
            datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
            ByteBuffer byteBuffer = ByteBuffer.wrap(message.getBytes());
            InetSocketAddress inetSocketAddress = new InetSocketAddress(ip, port);
            datagramChannel.send(byteBuffer, inetSocketAddress);
        }
    }

    private List<NetworkInterface> listAllMulticastInterfaces() {
        List<NetworkInterface> ret = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp() || !networkInterface.supportsMulticast()) {
                    continue;
                }

                ret.add(networkInterface);
            }
        } catch (SocketException e) {
            log.error(e.getMessage(), e);
        }
        return ret;
    }

}
