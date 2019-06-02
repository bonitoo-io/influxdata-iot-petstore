package io.bonitoo.demo.device;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDiscovery extends Thread {

    private static Logger log = LoggerFactory.getLogger(DeviceDiscovery.class);
    private Device device;
    private NetworkInterface networkInterface;

    public DeviceDiscovery(final Device device, final NetworkInterface networkInterface) {
        this.device = device;
        this.networkInterface = networkInterface;
    }

    public void run() {
        log.info("Starting Hub discovery...");
        if (device.isRegistered() && device.getHubApiUrl() != null) {
            return;
        }

        try {
            String received = receiveMessage(
                System.getProperty("petstore.multicastAddress", "230.0.0.0"),
                networkInterface.getName(),
                Integer.parseInt(System.getProperty("petstore.multicastPort", "4445")));

            String packetStartMark = "[petstore.hubUrl=";
            String url = received.substring(
                received.indexOf(packetStartMark) + packetStartMark.length(),
                received.indexOf("]"));

            log.info("hubUrl discovered: " + url + " !");
            if (device != null) {
                device.setHubApiUrl(url);
            }
        } catch (Exception e) {
            log.warn("iface: " + networkInterface.getName() + " ," + e.getMessage());
        }
    }


    private String receiveMessage(String ip, String iface, int port) throws IOException {

        try (DatagramChannel datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)) {
            log.info("Listening on " + iface + " ip:" + ip + ":" + port);
            NetworkInterface networkInterface = NetworkInterface.getByName(iface);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(port));
            datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);

            InetAddress inetAddress = InetAddress.getByName(ip);
            MembershipKey membershipKey = datagramChannel.join(inetAddress, networkInterface);
            log.info("Waiting for the message... " + networkInterface + " " + inetAddress);
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            datagramChannel.receive(byteBuffer);
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.limit()];
            byteBuffer.get(bytes, 0, byteBuffer.limit());
            membershipKey.drop();
            return new String(bytes);
        }
    }

    static List<NetworkInterface> listAllMulticastInterfaces() {
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


