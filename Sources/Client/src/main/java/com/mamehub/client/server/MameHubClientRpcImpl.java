package com.mamehub.client.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.MameHubEngine;
import com.mamehub.client.Utils;
import com.mamehub.client.utility.HexStringOutputStream;
import com.mamehub.rpc.MameHubClientRpc;
import com.mamehub.thrift.DownloadableRomState;
import com.mamehub.thrift.FileRequest;
import com.mamehub.thrift.FileResponse;
import com.mamehub.thrift.FileResponseCode;
import com.mamehub.thrift.PeerFileInfo;

public class MameHubClientRpcImpl implements MameHubClientRpc.Iface {
  final Logger logger = LoggerFactory.getLogger(MameHubClientRpcImpl.class);
  private static long lastRomUpdateTime = 0L;
  private static Map<String, Set<String>> downloadableRoms = null;
  public static MameHubEngine mameHubEngine;
  private long sentFileChunkTime = System.currentTimeMillis();
  private boolean couldStartNewUploadLastTime = false;

  @Override
  public boolean ping() throws TException {
    return true;
  }

  @Override
  public DownloadableRomState getDownloadableRoms(long lastCheckTime)
      throws TException {
    if (canStartNewUpload()) {
      if (lastCheckTime < lastRomUpdateTime) {
        // logger.info("Sending complete rom state: " +
        // downloadableRoms.size());
        return new DownloadableRomState(downloadableRoms, false);
      } else {
        return new DownloadableRomState(new HashMap<String, Set<String>>(),
            true);
      }
    }
    return new DownloadableRomState(new HashMap<String, Set<String>>(), false);
  }

  public static void updateRoms(Map<String, Set<String>> downloadableRoms) {
    MameHubClientRpcImpl.downloadableRoms = downloadableRoms;
    MameHubClientRpcImpl.lastRomUpdateTime = System.currentTimeMillis();
  }

  @Override
  public Set<String> requestRoms(String system, Set<String> romNames)
      throws TException {
    Set<String> response = new HashSet<String>();
    if (!canUpload()) {
      return response;
    }
    for (String romName : romNames) {
      if (MameHubClientRpcImpl.downloadableRoms != null
          && MameHubClientRpcImpl.downloadableRoms.containsKey(system)
          && MameHubClientRpcImpl.downloadableRoms.get(system)
              .contains(romName)) {
        response.add(romName);
      }
    }
    return response;
  }

  private File getFile(String system, String romName, int index) {
    if (!canUpload()) {
      return null;
    }
    if (system.equalsIgnoreCase("arcade")) {
      logger.info("Got arcade: " + mameHubEngine.getMameRomInfo(romName));
      return new File(
          mameHubEngine.getMameRomInfo(romName).filenames.get(index));
    } else if (system.equalsIgnoreCase("bios")) {
      logger.info("Got bios: " + mameHubEngine.getMessRomInfo(romName));
      return new File(
          mameHubEngine.getMessRomInfo(romName).filenames.get(index));
    } else {
      logger.info("Got cart: " + mameHubEngine.getCart(system, romName));
      return new File(
          mameHubEngine.getCart(system, romName).filenames.get(index));
    }
  }

  @Override
  public int getFileCount(String system, String romName) throws TException {
    if (system.equalsIgnoreCase("arcade")) {
      return mameHubEngine.getMameRomInfo(romName).filenames.size();
    } else if (system.equalsIgnoreCase("bios")) {
      return mameHubEngine.getMessRomInfo(romName).filenames.size();
    } else {
      return mameHubEngine.getCart(system, romName).filenames.size();
    }
  }

  @Override
  public PeerFileInfo getFileInfo(String system, String romName, int index)
      throws TException {
    if (!canUpload()) {
      return new PeerFileInfo();
    }
    File f = getFile(system, romName, index);
    return new PeerFileInfo(f.getName(), f.length());
  }

  @Override
  public FileResponse getFileChunk(FileRequest request) throws TException {
    if (!canUpload()) {
      throw new TException("Host entered game");
    }
    sentFileChunkTime = System.currentTimeMillis();
    FileInputStream fis = null;
    try {
      System.out.println("Sending file chunk...");
      File file = getFile(request.requestSystem, request.requestRom,
          request.fileIndex);
      logger.info("Sending chunk of file " + file);
      fis = new FileInputStream(file);
      if (request.byteOffset != fis.skip(request.byteOffset)) {
        throw new RuntimeException("COULD NOT SKIP CORRECT NUMBER OF BYTES");
      }

      FileResponse response = new FileResponse();

      byte[] b = new byte[request.chunkSize];
      int bytesRead = fis.read(b);
      response.code = FileResponseCode.OK;
      if (bytesRead < 0) {
        throw new RuntimeException("OOPS!");
      } else if (bytesRead < request.chunkSize) {
        response.code = FileResponseCode.EOF;
      }
      HexStringOutputStream hsos = new HexStringOutputStream();
      hsos.write(b, 0, bytesRead);
      response.dataHex = hsos.toString();
      hsos.close();
      return response;
    } catch (IOException e) {
      throw new TException(e);
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (IOException e) {
      }
    }
  }

  private boolean canUpload() {
    if (Utils.getApplicationSettings().allowUploading == false) {
      // logger.info("Allow uploading turned off");
      return false;
    }
    boolean canUpload = !(mameHubEngine == null || downloadableRoms == null);
    if (!canUpload) {
      // logger.info("Can't upload because " + (mameHubEngine == null) + " or "
      // + mameHubEngine.isGameRunning());
    }
    return canUpload;
  }

  private boolean canStartNewUpload() {
    boolean canStartNewUpload = canUpload() && !mameHubEngine.isGameRunning()
        && System.currentTimeMillis() - 10000 > sentFileChunkTime;
    if (canStartNewUpload != couldStartNewUploadLastTime) {
      lastRomUpdateTime = System.currentTimeMillis();
      couldStartNewUploadLastTime = canStartNewUpload;
    }
    return canStartNewUpload;
  }
}
