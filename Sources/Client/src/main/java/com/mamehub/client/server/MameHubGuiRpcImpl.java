package com.mamehub.client.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.MameHubEngine;
import com.mamehub.rpc.MameHubGuiRpc;

public class MameHubGuiRpcImpl implements MameHubGuiRpc.Iface {
  final Logger logger = LoggerFactory.getLogger(MameHubGuiRpcImpl.class);
  private static long lastRomUpdateTime = 0L;
  private static Map<String, Set<String>> downloadableRoms = null;
  public static MameHubEngine mameHubEngine;
  private long sentFileChunkTime = System.currentTimeMillis();
  private boolean couldStartNewUploadLastTime = false;

  @Override
  public Map<String, Integer> getPings() throws TException {
    HashMap<String, Integer> m = new HashMap<>();
    m.put("Test", 1);
    return m;
  }

}
