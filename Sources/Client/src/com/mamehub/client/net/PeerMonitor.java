package com.mamehub.client.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.impl.client.ContentEncodingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.transport.THttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.MainFrame;
import com.mamehub.client.MameHubEngine;
import com.mamehub.client.Utils;
import com.mamehub.client.utility.HexStringInputStream;
import com.mamehub.rpc.MameHubClientRpc;
import com.mamehub.thrift.DownloadableRomState;
import com.mamehub.thrift.FileRequest;
import com.mamehub.thrift.FileResponse;
import com.mamehub.thrift.FileResponseCode;
import com.mamehub.thrift.PeerFileInfo;
import com.mamehub.thrift.PeerState;
import com.mamehub.thrift.Player;
import com.mamehub.thrift.SystemRomPair;

public class PeerMonitor implements Runnable {
	final Logger logger = LoggerFactory.getLogger(PeerMonitor.class);

	public class RomDownloadState implements Runnable {
		Map<Long, byte[]> outOfOrderPieces;
		private ArrayList<Player> playersWithRom;
		public boolean failed = false;
		public boolean finished = false;
		private SystemRomPair systemRomPair;
		public PeerFileInfo fileInfo;
		long offset = 0L;
		public boolean cancel = false;

		public RomDownloadState(SystemRomPair systemRomPair,
				Set<Player> playersWithRom) {
			this.systemRomPair = systemRomPair;
			this.playersWithRom = new ArrayList<Player>(playersWithRom);
			Collections.shuffle(this.playersWithRom);
			new Thread(this).start();
		}

		@Override
		public void run() {
			for (Player player : playersWithRom) {
				fileInfo = null;
				logger.debug("TRYING TO GET " + systemRomPair + " FROM "
						+ player);
				THttpClient gameTransport = null;
				try {
					// set the connection timeout value to 3 seconds (3000
					// milliseconds)
					final HttpParams httpParams = new BasicHttpParams();
					HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
					DefaultHttpClient client = new ContentEncodingHttpClient(
							httpParams);

					gameTransport = new THttpClient("http://"
							+ player.ipAddress + ":" + player.basePort
							+ "/mamehubclient", client);
					gameTransport.open();
					TJSONProtocol gameProtocol = new TJSONProtocol(
							gameTransport);
					MameHubClientRpc.Client gameClient = new MameHubClientRpc.Client(
							gameProtocol);

					fileInfo = gameClient.getFileInfo(systemRomPair.system,
							systemRomPair.rom);
					if (fileInfo.length == 0) {
						logger.debug("Could not get from " + player.name);
						continue;
					}
					File file = new File("../roms/" + fileInfo.filename);
					FileOutputStream outputStream = new FileOutputStream(file);

					offset = 0L;
					int chunkSize = 4096;
					long curTime = System.currentTimeMillis();
					boolean transferFailed = false;
					byte[] b = new byte[128 * 1024];
					while (!cancel) {
						curTime = System.currentTimeMillis();
						FileResponse fr = gameClient
								.getFileChunk(new FileRequest(
										systemRomPair.rom,
										systemRomPair.system, offset, chunkSize));
						if (fr.code == FileResponseCode.ERROR) {
							transferFailed = true;
							break;
						}
						HexStringInputStream hsis = new HexStringInputStream(
								fr.dataHex);
						while (true) {
							int bytesRead = hsis.read(b);
							logger.debug("BYTES READ: " + bytesRead);
							if (bytesRead < 0)
								break;
							offset += bytesRead;
							outputStream.write(b, 0, bytesRead);
						}
						if (fr.code == FileResponseCode.EOF) {
							break;
						}
						if (curTime + 1000 < System.currentTimeMillis()
								&& chunkSize > 1024) {
							// Took longer than a second, cut back
							chunkSize /= 2;
						} else if (curTime + 500 > System.currentTimeMillis()) {
							// Finished too soon, speed up
							chunkSize *= 2;
						}
						hsis.close();
						updateUI(false);
						Thread.sleep(500);
					}
					outputStream.close();
					if (cancel || transferFailed) {
					} else {
						if (file.getName().toLowerCase().endsWith(".zip")) {
							try {
								// Make sure download was successful
								ZipFile zf = new ZipFile(file);
								for (Enumeration<? extends ZipEntry> e = zf
										.entries(); e.hasMoreElements();) {
									ZipEntry ze = e.nextElement();

									long expectedCrc = ze.getCrc();

									byte[] data = new byte[1 * 1024 * 1024];
									CRC32 crc32 = new CRC32();

									int nRead;

									InputStream is = zf.getInputStream(ze);
									while ((nRead = is.read(data, 0,
											data.length)) != -1) {
										crc32.update(data, 0, nRead);
									}
									is.close();

									if (expectedCrc != crc32.getValue()) {
										transferFailed = true;
									}
								}
							} catch (IOException e) {
								transferFailed = true;
							}
						}
					}
					if (cancel || transferFailed) {
						file.renameTo(new File(file.getAbsolutePath() + ".bad"));
					}
					if (cancel)
						return;
					if (transferFailed)
						continue; // Try another host
					finished = true;
					updateUI(true);
					return;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (gameTransport != null) {
						gameTransport.close();
					}
				}
			}
			logger.debug("FAILED TO GET " + systemRomPair);
			failed = true;
			updateUI(false);
		}

		private void updateUI(boolean justFinished) {
			synchronized (PeerMonitor.this) {
				boolean allDone = true;
				Map<RomDownloadState, String> downloadState = new HashMap<RomDownloadState, String>();
				for (RomDownloadState state : requests.values()) {
					String value = String.valueOf(state.getPercentComplete());
					if (state.failed) {
						value = "FAILED";
					} else if (state.finished) {
						value = "FINISHED";
					} else {
						allDone = false;
					}
					downloadState.put(state, value);
				}
				listener.updateDownloads(downloadState);
				if (allDone && justFinished) {
					mameHubEngine.startAudit(true);
				}
			}
		}

		private Double getPercentComplete() {
			if (fileInfo == null)
				return 0.0;
			return (offset * 100.0) / fileInfo.length;
		}
	}

	private ConcurrentMap<String, PeerState> idStateMap = new ConcurrentHashMap<String, PeerState>();
	private ConcurrentMap<SystemRomPair, RomDownloadState> requests = new ConcurrentHashMap<SystemRomPair, RomDownloadState>();
	private Thread thread;
	private PeerMonitorListener listener;
	private boolean gotNewRoms;
	private MameHubEngine mameHubEngine;

	public interface PeerMonitorListener {
		public void statesUpdated();

		public void updateCloudRoms(Map<String, Set<String>> downloadableRoms);

		public void updateDownloads(Map<RomDownloadState, String> downloadStatus);
	}

	public PeerMonitor(PeerMonitorListener listener, MameHubEngine mameHubEngine) {
		this.listener = listener;
		this.mameHubEngine = mameHubEngine;
		thread = new Thread(this);
		thread.start();
	}

	public boolean containsPeer(Player peer) {
		return idStateMap.containsKey(peer.id);
	}

	public void insertPeer(Player peer) {
		synchronized (idStateMap) {
			// Just pull out the parts of the player we care about
			idStateMap.put(peer.id, new PeerState());
		}
	}

	public void insertAll(Collection<Player> values) {
		for (Player p : values) {
			insertPeer(p);
		}
	}

	public PeerState getPeer(Player player) {
		return idStateMap.get(player.id);
	}

	public void removePeer(Player peer) {
		synchronized (idStateMap) {
			idStateMap.remove(peer.id);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				gotNewRoms = false;
				Set<String> currentPlayers;
				synchronized (idStateMap) {
					currentPlayers = new HashSet<String>(idStateMap.keySet());
				}
				for (String playerId : currentPlayers) {
					Player player = MainFrame.knownPlayers.get(playerId);
					PeerState peerState = idStateMap.get(playerId);
					if (peerState == null) {
						// This player was deleted for some reason.
						continue;
					}

					logger.debug("CHECKING " + player);
					updatePeerData(player, peerState);
					Thread.sleep(100);
				}
				listener.statesUpdated();
				if (gotNewRoms) {
					synchronized (idStateMap) {
						Map<String, Set<String>> combinedCloud = new HashMap<String, Set<String>>();
						for (PeerState ps : idStateMap.values()) {
							for (Entry<String, Set<String>> entry : ps.downloadableRoms
									.entrySet()) {
								if (!combinedCloud.containsKey(entry.getKey())) {
									combinedCloud.put(
											entry.getKey(),
											new HashSet<String>(entry
													.getValue()));
								} else {
									combinedCloud.get(entry.getKey()).addAll(
											entry.getValue());
								}
							}
						}
						listener.updateCloudRoms(combinedCloud);
					}
				}
				Thread.sleep(1000 * 15);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updatePeerData(Player player, PeerState peerState) {
		if (player == null) {
			return;
		}
		if (MainFrame.myPlayerId != null && player.id.equals(MainFrame.myPlayerId)) {
			// Don't try to connect to yourself
			return;
		}
		THttpClient gameTransport = null;
		try {
			// set the connection timeout value to 3 seconds (3000 milliseconds)
			final HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
			DefaultHttpClient client = new ContentEncodingHttpClient(httpParams);

			gameTransport = new THttpClient("http://" + player.ipAddress + ":"
					+ player.basePort + "/mamehubclient", client);
			gameTransport.open();
			TJSONProtocol gameProtocol = new TJSONProtocol(gameTransport);
			MameHubClientRpc.Client gameClient = new MameHubClientRpc.Client(
					gameProtocol);
			long curtime = System.currentTimeMillis();
			if (gameClient.ping()) {
				logger.debug("Setting ping for " + player);
				peerState.ping = (int) (System.currentTimeMillis() - curtime);

				// Hack to account for http overhead
				peerState.ping /= 2;

				DownloadableRomState romState = gameClient
						.getDownloadableRoms(peerState.lastCheckTime);
				logger.debug("ROM STATE: " + romState.roms.size() + " "
						+ romState.stale);
				if (romState.stale == false) {
					gotNewRoms = true;
					peerState.downloadableRoms = Utils
							.getApplicationDatabaseEngine()
							.getOrCreatePrimitiveMap(
									"DownloadableRoms" + player.id);
					peerState.downloadableRoms.clear();
					peerState.downloadableRoms.putAll(romState.roms);
					peerState.lastCheckTime = System.currentTimeMillis();
					Utils.getApplicationDatabaseEngine().commit();
				}
			} else {
				logger.error("Could not ping " + player + "!!!");
				throw new RuntimeException("OOPS");
			}
		} catch (TException tte) {
			if (player.portsOpen) {
				logger.warn("WARNING: Could not reach " + player.id
						+ " even though ports were open.");
			}
			// We couldn't get through the client's firewall
			peerState.ping = -1;
		} finally {
			if (gameTransport != null) {
				gameTransport.close();
			}
		}
	}

	public void updatePeer(Player playerChanged) {
		// TODO Auto-generated method stub

	}

	public boolean requestRoms(String system, Set<String> romsNeeded,
			String chdName, Player fallbackPlayer) {
		Map<String, Set<Player>> romPlayerMap = new HashMap<String, Set<Player>>();
		for (String romNeeded : romsNeeded) {
			romPlayerMap.put(romNeeded, new HashSet<Player>());
			synchronized (idStateMap) {
				for (Entry<String, PeerState> entry : idStateMap.entrySet()) {
					Player p = MainFrame.knownPlayers.get(entry.getKey());
					PeerState ps = entry.getValue();
					logger.debug(ps.toString() + " " + system + " "
							+ ps.downloadableRoms.containsKey(system));
					if (ps.downloadableRoms.containsKey(system)
							&& ps.downloadableRoms.get(system).contains(
									romNeeded)) {
						romPlayerMap.get(romNeeded).add(p);
					}
				}
			}
			if (romPlayerMap.get(romNeeded).isEmpty()) {
				if (fallbackPlayer == null) {
					return false;
				} else {
					romPlayerMap.get(romNeeded).add(fallbackPlayer);
				}
			}
		}

		// Take action (start requesting pieces)
		for (String romNeeded : romsNeeded) {
			SystemRomPair srp = new SystemRomPair(system, romNeeded);
			if (!requests.containsKey(srp) || requests.get(srp).failed
					|| requests.get(srp).cancel) {
				requests.put(srp,
						new RomDownloadState(srp, romPlayerMap.get(romNeeded)));
			}
		}

		if (chdName != null) {
			SystemRomPair srp = new SystemRomPair(system, chdName + "_chd");
			if (!requests.containsKey(srp)) {
				requests.put(srp,
						new RomDownloadState(srp, romPlayerMap.get(chdName)));
			}
		}

		return true;
	}

}
