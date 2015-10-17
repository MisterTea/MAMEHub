package com.mamehub.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.LogManager;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.login.LoginDialog;
import com.mamehub.client.server.ClientHttpServer;
import com.mamehub.client.server.GuiHttpServer;
import com.mamehub.client.upnp.PortOpener;
import com.mamehub.client.utility.CommandLineFlags;

public class MAMEHub {
	public static class ProcessKiller implements Runnable {
		final Logger logger = LoggerFactory.getLogger(ProcessKiller.class);

		private Process process;

		public ProcessKiller(Process process) {
			this.process = process;
		}

		@Override
		public void run() {
			logger.info("Killing subprocess");
			process.destroy();
		}

	}

	public static Thread portOpenerThread;

	public static class MemoryReporter implements Runnable {
		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
				Utils.dumpMemoryUsage();
			}
		}
	}
	
	/**
	 * Launch the application.
	 * 
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IllegalArgumentException,
			SecurityException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException, IOException {
		final InputStream inputStream = MAMEHub.class
				.getResourceAsStream("/log.properties");
		try {
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (final IOException e) {
			java.util.logging.Logger.getAnonymousLogger().severe(
					"Could not load default logging.properties file");
			java.util.logging.Logger.getAnonymousLogger().severe(e.getMessage());
		}
		
		// Ensure that databases are made in the main thread.
		Utils.getConfiguration();
		Utils.getAuditDatabaseEngine();

		new CommandLineFlags(args);
		new SoundEngine();

		// new Thread(new MemoryReporter()).start();

		// make ume.ini if it doesn't exist
		if (!new File("ume.ini").exists()) {
				if (Utils.isMac()) {
						FileUtils.copyFile(new File("ume.ini.osx"), new File("ume.ini"));
				} else if (Utils.isUnix()) {
						FileUtils.copyFile(new File("ume.ini.unix"), new File("ume.ini"));
				} else {
						FileUtils.copyFile(new File("ume.ini.win"), new File("ume.ini"));
				}
		}

		try {
			portOpenerThread = new Thread(new PortOpener(Utils.getConfiguration().getInt("basePort"), Utils.getConfiguration().getInt("secondaryPort")));
			portOpenerThread.start();
			ClientHttpServer clientHttpServer = new ClientHttpServer(Utils.getConfiguration().getInt("basePort"));
			GuiHttpServer guiHttpServer = new GuiHttpServer(8914);
			LoginDialog dialog = new LoginDialog(clientHttpServer, guiHttpServer);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
