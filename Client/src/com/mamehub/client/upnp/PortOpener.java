package com.mamehub.client.upnp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.UpnpServiceImpl;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.igd.PortMappingListener;
import org.fourthline.cling.support.model.PortMapping;
import org.fourthline.cling.transport.impl.apache.StreamClientConfigurationImpl;
import org.fourthline.cling.transport.impl.apache.StreamClientImpl;
import org.fourthline.cling.transport.impl.apache.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.impl.apache.StreamServerImpl;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamClient;
import org.fourthline.cling.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a simple UPnP discovery procedure.
 */
public class PortOpener implements Runnable {
	public class ExitClingShutdownHook implements Runnable {

		@Override
		public void run() {
			logger.info("Stopping Cling...");
			// Release all resources and advertise BYEBYE to other UPnP devices
			upnpService.shutdown();
		}

	}

	final Logger logger = LoggerFactory.getLogger(PortOpener.class);
	UpnpService upnpService;

	public class MyUpnpServiceConfiguration extends
			DefaultUpnpServiceConfiguration {

		@Override
		public StreamClient<StreamClientConfigurationImpl> createStreamClient() {
			return new StreamClientImpl(new StreamClientConfigurationImpl(
					getSyncProtocolExecutorService()));
		}

		@Override
		public StreamServer<StreamServerConfigurationImpl> createStreamServer(
				NetworkAddressFactory networkAddressFactory) {
			return new StreamServerImpl(new StreamServerConfigurationImpl(
					networkAddressFactory.getStreamListenPort()));
		}

	}

	@Override
	public void run() {
		// UPnP discovery is asynchronous, we need a callback
		RegistryListener registryListener = new RegistryListener() {

			@Override
			public void remoteDeviceDiscoveryStarted(Registry registry,
					RemoteDevice device) {
				logger.debug("Discovery started: " + device.getDisplayString());
			}

			@Override
			public void remoteDeviceDiscoveryFailed(Registry registry,
					RemoteDevice device, Exception ex) {
				logger.debug("Discovery failed: " + device.getDisplayString()
						+ " => " + ex);
			}

			@Override
			public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
				logger.info("Remote device available: "
						+ device.getDisplayString());

			}

			@Override
			public void remoteDeviceUpdated(Registry registry,
					RemoteDevice device) {
				logger.debug("Remote device updated: "
						+ device.getDisplayString());
			}

			@Override
			public void remoteDeviceRemoved(Registry registry,
					RemoteDevice device) {
				logger.info("Remote device removed: "
						+ device.getDisplayString());
			}

			@Override
			public void localDeviceAdded(Registry registry, LocalDevice device) {
				logger.info("Local device added: " + device.getDisplayString());
			}

			@Override
			public void localDeviceRemoved(Registry registry, LocalDevice device) {
				logger.info("Local device removed: "
						+ device.getDisplayString());
			}

			@Override
			public void beforeShutdown(Registry registry) {
				logger.info("Before shutdown, the registry has devices: "
						+ registry.getDevices().size());
			}

			@Override
			public void afterShutdown() {
				logger.info("Shutdown of registry complete!");

			}
		};

		// This will create necessary network resources for UPnP right away
		logger.info("Starting Cling...");
		upnpService = new UpnpServiceImpl(new MyUpnpServiceConfiguration(),
				registryListener);

		// Get internal IP address
		String ipAddress = null;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface
					.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface current = interfaces.nextElement();
				if (!current.isUp() || current.isLoopback()
						|| current.isVirtual())
					continue;
				Enumeration<InetAddress> addresses = current.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress current_addr = addresses.nextElement();
					if (!(current_addr instanceof Inet4Address)) {
						// Sorry, no ipv6 support (yet)
						continue;
					}
					if (current_addr.isLoopbackAddress())
						continue;
					ipAddress = current_addr.getHostAddress();
				}
			}
			logger.info("HOST IP ADDRESS IS " + ipAddress);
		} catch (SocketException se) {
			throw new RuntimeException(se);
		}

		int port1 = 6805;
		int port2 = 6806;
		PortMapping tcpMapping = new PortMapping(port1, ipAddress,
				PortMapping.Protocol.TCP, "MAMEHub TCP Port Mapping 1");
		upnpService.getRegistry().addListener(
				new PortMappingListener(tcpMapping));
		PortMapping tcpMapping2 = new PortMapping(port2, ipAddress,
				PortMapping.Protocol.TCP, "MAMEHub TCP Port Mapping 2");
		upnpService.getRegistry().addListener(
				new PortMappingListener(tcpMapping2));
		PortMapping udpMapping = new PortMapping(port1, ipAddress,
				PortMapping.Protocol.UDP, "MAMEHub UDP Port Mapping 1");
		upnpService.getRegistry().addListener(
				new PortMappingListener(udpMapping));
		PortMapping udpMapping2 = new PortMapping(port2, ipAddress,
				PortMapping.Protocol.UDP, "MAMEHub UDP Port Mapping 2");
		upnpService.getRegistry().addListener(
				new PortMappingListener(udpMapping2));

		// Send a search message to all devices and services, they should
		// respond soon
		upnpService.getControlPoint().search(new STAllHeader());

		// Exit cling when closing the app
		Runtime.getRuntime().addShutdownHook(
				new Thread(new ExitClingShutdownHook()));
	}

}
