package com.mamehub.client;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.login.LoginDialog;
import com.mamehub.client.server.ClientHttpServer;
import com.mamehub.client.upnp.PortOpener;
import com.mamehub.client.utility.CommandLineFlags;
import com.mamehub.client.utility.SwtLoader;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;

public class Main {
	public static class ProcessKiller implements Runnable {
		final Logger logger = LoggerFactory.getLogger(ProcessKiller.class);
		
		private Process process;

		public ProcessKiller(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			logger.info("Kiling subprocess");
			process.destroy();
		}
		
	}
	
	public static Thread portOpenerThread;

	/**
	 * Launch the application.
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		new SwtLoader();
		new CommandLineFlags(args);
		new SoundEngine();
		
		Class<?> c = Class.forName("org.eclipse.swt.widgets.Display");
		c.getMethod("setAppName", String.class).invoke(null, "MAMEHub");
		
	    UIUtils.setPreferredLookAndFeel();
	    NativeInterface.open();
	    
		try {
			portOpenerThread = new Thread(new PortOpener());
			portOpenerThread.start();
			if(Utils.isWindows()) {
				Process mumbleProcess = Runtime.getRuntime().exec("..\\MumblePortable\\MumblePortable.exe mumble://ucfpawn.dyndns.info:64738/?version=1.2.2");
				Runtime.getRuntime().addShutdownHook(new Thread(new ProcessKiller(mumbleProcess)));
			}
			ClientHttpServer clientHttpServer = new ClientHttpServer();
			LoginDialog dialog = new LoginDialog(clientHttpServer);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	    NativeInterface.runEventPump();
	}
}
