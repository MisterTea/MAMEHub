package com.mamehub.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.audit.GameAuditor;
import com.mamehub.client.audit.GameAuditor.RomQueryResult;
import com.mamehub.client.server.MameHubClientRpcImpl;
import com.mamehub.client.utility.MediaCommandFetcher;
import com.mamehub.client.utility.OSValidator;
import com.mamehub.thrift.RomInfo;

public class MameHubEngine implements Runnable {
	final Logger logger = LoggerFactory.getLogger(MameHubEngine.class);
	
	private Thread gameThread;
	private Process proc;
	private GameAuditor gameAuditor;
	
	MediaCommandFetcher mediaCommandFetcher;
	
	public interface EmulatorHandler {
		public void gameFinished(int returnCode, File outputFile);

		public void inGameException(Exception e);
	}
	EmulatorHandler handler;
	private String romFileName;
	private boolean quit = false;

	public class ExitMameShutdownHook implements Runnable {

		@Override
		public void run() {
			quit = true;
			if(gameAuditor != null) {
				logger.info("CANCELLING AUDIT");
				gameAuditor.cancelAudit();
			}
			try {
				if(gameThread != null)
					gameThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(proc != null) {
				proc.destroy();
			}
			logger.info("MAME SHUTDOWN COMPLETE");
			
			Utils.shutdownDatabaseEngine();
		}
		
	}
	
	public MameHubEngine(GameAuditor.AuditHandler ah, EmulatorHandler eh) throws IOException {
		this.handler = eh;
		
		mediaCommandFetcher = new MediaCommandFetcher(Utils.getResource(MameHubEngine.class, "/media.txt"));
		
		gameAuditor = new GameAuditor(ah);

		Runtime.getRuntime().addShutdownHook(new Thread(new ExitMameShutdownHook()));
		SoundEngine.instance.mameHubEngine  = this;
		MameHubClientRpcImpl.mameHubEngine = this;
	}
	
	public boolean startAudit(boolean runScanner) {
		return gameAuditor.startAudit(runScanner);
	}

	public boolean launchGame(String username, String systemName, RomInfo romOrCart, boolean isServer, String ipAddress, int selfport, int port) throws IOException {
		if(isGameRunning()) {
			return false;
		}
		
		List<String> cmdList = new ArrayList<String>();
		if(new File("./csume64").exists() || new File("./csume64.exe").exists()) {
			if(OSValidator.isWindows()) {
				cmdList.add("csume64.exe");
			} else {
				cmdList.add("./csume64");
			}
		} else {
			if(OSValidator.isWindows()) {
				cmdList.add("csume.exe");
			} else {
				cmdList.add("./csume");
			}
		}
		
		if(systemName.equalsIgnoreCase("arcade")) {
		  // Pick the first non-CHD file
		  for (String s : romOrCart.filenames) {
		    if (!s.toLowerCase().endsWith(".chd")) {
		      romFileName = s;
		      break;
		    }
		  }
			cmdList.add("\"" + romFileName + "\"");
		} else {
			// Before we passed the bios path to ume, now we need to pass the system name.
			// this.romFileName = getMessRomInfo(systemName).filename;
			this.romFileName = systemName;
			cmdList.add(romFileName);
			for (Entry<String, String> interfaceTypeNamePair : romOrCart.interfaceFileMap.entrySet()) {
				cmdList.add("-" + interfaceTypeNamePair.getKey());
                //cmdList.add(interfaceTypeNamePair.getValue());
                cmdList.add(romOrCart.id);
			}
		}

		cmdList.add("-port");
		cmdList.add(Integer.toString(port));
		if(isServer) {
			cmdList.add("-server");
		} else {
			cmdList.add("-client");
			cmdList.add("-selfport");
			cmdList.add(Integer.toString(selfport));
			cmdList.add("-hostname");
			cmdList.add(ipAddress);
		}
		cmdList.add("-username");
		cmdList.add(username);
		
		String outs = "CALLING: ";
		for(String s : cmdList) {
			outs += s + " ";
		}
		logger.info(outs);
		proc = Runtime.getRuntime().exec(cmdList.toArray(new String[cmdList.size()]));
		
		gameThread = new Thread(this);
		gameThread.start();
		return true;
	}

	@Override
	public void run() {
		if(!new File("../logs").exists()) {
			new File("../logs").mkdir();
		}
		File outputFile = new File("../logs/"+new File(romFileName).getName()+"_"+(System.currentTimeMillis()/1000)%(60*24*365)+".txt")
				.getAbsoluteFile();
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile);
			int result = -999;
			while(result==-999 && !quit) {
				while(proc.getErrorStream().available()>0) {
					int i = proc.getErrorStream().read();
					if(i>=0 && i<128) {
						writer.write(i);
					}
				}
				while(proc.getInputStream().available()>0) {
					int i = proc.getInputStream().read();
					if(i>=0 && i<128) {
						writer.write(i);
					}
				}
				writer.flush();
				try {
					result = proc.exitValue();
				} catch(IllegalThreadStateException e) {
					// Ignore, wait for process to finish
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			if(quit) {
				return;
			}
	        logger.info("RESULT: " + result);
	        
			while(proc.getErrorStream().available()>0) {
				int i = proc.getErrorStream().read();
				if(i>=0 && i<128) {
					writer.write(i);
				}
			}
			while(proc.getInputStream().available()>0) {
				int i = proc.getInputStream().read();
				if(i>=0 && i<128) {
					writer.write(i);
				}
			}
			
			handler.gameFinished(result, outputFile);
			proc = null;
			return;
		} catch(IOException e) {
			handler.inGameException(e);
		} finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		handler.gameFinished(-1, outputFile);
	}

	public RomInfo getMameRomInfo(String romName) {
		return gameAuditor.getMameRomInfoMap().get(romName);
	}
	
	public RomInfo getMessRomInfo(String romName) {
		return gameAuditor.getMessRomInfoMap().get(romName);
	}
	
	public RomInfo getCart(String systemName, String cartName) {
		return gameAuditor.getSystemRomInfoMap(systemName).get(cartName);
	}

	public void cancelAudit() {
		gameAuditor.cancelAudit();
	}

	public boolean isAuditing() {
		return gameAuditor.isAuditing();
	}
	
	public boolean isGameRunning() {
		if(gameThread != null && gameThread.isAlive()) {
			return true;
		}
		return false;
	}

	public List<RomQueryResult> queryRoms(String text,
			Map<String, Set<String>> cloudRoms) {
		return gameAuditor.queryRoms(text, cloudRoms);
	}

	public ConcurrentMap<String, RomInfo> getMameRomInfoMap() {
		if (isAuditing()) {
			return new ConcurrentHashMap<String, RomInfo>();
		} else {
			return gameAuditor.getMameRomInfoMap();
		}
	}

	public ConcurrentMap<String, RomInfo> getMessRomInfoMap() {
		if (isAuditing()) {
			return new ConcurrentHashMap<String, RomInfo>();
		} else {
			return gameAuditor.getMessRomInfoMap();
		}
	}

	public ConcurrentMap<String, RomInfo> getSystemRomInfoMap(String system) {
		if (isAuditing()) {
			return new ConcurrentHashMap<String, RomInfo>();
		} else {
			return gameAuditor.getSystemRomInfoMap(system);
		}
	}
}
