package io.bonitoo.demo.device;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDiscovery extends Thread {

    private static Logger log = LoggerFactory.getLogger(DeviceDiscovery.class);
    protected DatagramSocket socket = null;
    protected boolean running;
    protected byte[] buf = new byte[256];
    Device device;

    public DeviceDiscovery(Device device) throws IOException {
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(4445));
        this.device = device;
    }

    public void run() {
        log.info("Starting Hub discovery...");
        running = true;
        if (device.isRegistered()) {
            return;
        }

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                String received = new String(packet.getData(), 0, packet.getLength());

                String packetStartMark = "[petstore.hubUrl=";
                String url = received.substring(
                    received.indexOf(packetStartMark) + packetStartMark.length(),
                    received.indexOf("]"));

                if (url != null) {
                    log.info("hubUrl discovered: " + url + " !");
                    running = false;
                    if (device != null) {
                        device.setHubApiUrl(url);
                    }
                }
                socket.send(packet);
                Thread.sleep(1000L);
            } catch (Exception e) {
                e.printStackTrace();
                running = false;
            }
        }
        socket.close();
    }
}


