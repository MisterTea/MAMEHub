package com.mamehub.client.upnp;

import java.net.InetAddress;
import org.teleal.cling.DefaultUpnpServiceConfiguration;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.igd.PortMappingListener;
import org.teleal.cling.support.model.PortMapping;
import org.teleal.cling.transport.impl.apache.StreamClientConfigurationImpl;
import org.teleal.cling.transport.impl.apache.StreamClientImpl;
import org.teleal.cling.transport.impl.apache.StreamServerConfigurationImpl;
import org.teleal.cling.transport.impl.apache.StreamServerImpl;
import org.teleal.cling.transport.spi.NetworkAddressFactory;
import org.teleal.cling.transport.spi.StreamClient;
import org.teleal.cling.transport.spi.StreamServer;
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

	public class MyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

	    @Override
	    public StreamClient<StreamClientConfigurationImpl> createStreamClient() {
	        return new StreamClientImpl(new StreamClientConfigurationImpl());
	    }

	    @Override
	    public StreamServer<StreamServerConfigurationImpl> createStreamServer(NetworkAddressFactory networkAddressFactory) {
	        return new StreamServerImpl(
	                new StreamServerConfigurationImpl(
	                        networkAddressFactory.getStreamListenPort()
	                )
	        );
	    }

	}
	
	@Override
	public void run() {
        // UPnP discovery is asynchronous, we need a callback
        RegistryListener registryListener = new RegistryListener() {

            @Override
			public void remoteDeviceDiscoveryStarted(Registry registry,
                                                     RemoteDevice device) {
                logger.debug(
                        "Discovery started: " + device.getDisplayString()
                );
            }

            @Override
			public void remoteDeviceDiscoveryFailed(Registry registry,
                                                    RemoteDevice device,
                                                    Exception ex) {
                logger.debug(
                        "Discovery failed: " + device.getDisplayString() + " => " + ex
                );
            }

            @Override
			public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                logger.info(
                        "Remote device available: " + device.getDisplayString()
                );
                
            }

            @Override
			public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                logger.debug(
                        "Remote device updated: " + device.getDisplayString()
                );
            }

            @Override
			public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                logger.info(
                        "Remote device removed: " + device.getDisplayString()
                );
            }

            @Override
			public void localDeviceAdded(Registry registry, LocalDevice device) {
                logger.info(
                        "Local device added: " + device.getDisplayString()
                );
            }

            @Override
			public void localDeviceRemoved(Registry registry, LocalDevice device) {
                logger.info(
                        "Local device removed: " + device.getDisplayString()
                );
            }

            @Override
			public void beforeShutdown(Registry registry) {
                logger.info(
                        "Before shutdown, the registry has devices: "
                        + registry.getDevices().size()
                );
            }

            @Override
			public void afterShutdown() {
                logger.info("Shutdown of registry complete!");

            }
        };

        // This will create necessary network resources for UPnP right away
        logger.info("Starting Cling...");
        upnpService = new UpnpServiceImpl(new MyUpnpServiceConfiguration(), registryListener);
        
        InetAddress myAddr = null;
        InetAddress[] it = upnpService.getRouter().getNetworkAddressFactory().getBindAddresses();
        for(InetAddress addr : it) {
        	logger.info("HOST ADDRESS: " + addr.getHostAddress());
        	myAddr = addr;
        }

        int port1 = 6805;
        int port2 = 6806;
        PortMapping tcpMapping =
                new PortMapping(
                        port1,
                        myAddr.getHostAddress(),
                        PortMapping.Protocol.TCP,
                        "MAMEHub TCP Port Mapping 1"
                );
        upnpService.getRegistry().addListener(new PortMappingListener(tcpMapping));
        PortMapping tcpMapping2 =
                new PortMapping(
                        port2,
                        myAddr.getHostAddress(),
                        PortMapping.Protocol.TCP,
                        "MAMEHub TCP Port Mapping 2"
                );
        upnpService.getRegistry().addListener(new PortMappingListener(tcpMapping2));
        PortMapping udpMapping =
                new PortMapping(
                        port1,
                        myAddr.getHostAddress(),
                        PortMapping.Protocol.UDP,
                        "MAMEHub UDP Port Mapping 1"
                );
        upnpService.getRegistry().addListener(new PortMappingListener(udpMapping));
        PortMapping udpMapping2 =
                new PortMapping(
                        port2,
                        myAddr.getHostAddress(),
                        PortMapping.Protocol.UDP,
                        "MAMEHub UDP Port Mapping 2"
                );
        upnpService.getRegistry().addListener(new PortMappingListener(udpMapping2));

        // Send a search message to all devices and services, they should respond soon
        upnpService.getControlPoint().search(new STAllHeader());

        // Exit cling when closing the app
		Runtime.getRuntime().addShutdownHook(new Thread(new ExitClingShutdownHook()));
    }
	
}

