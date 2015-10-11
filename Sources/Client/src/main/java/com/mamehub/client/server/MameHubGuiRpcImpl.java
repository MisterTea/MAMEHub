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
import com.mamehub.rpc.MameHubGuiRpc;
import com.mamehub.thrift.DownloadableRomState;
import com.mamehub.thrift.FileRequest;
import com.mamehub.thrift.FileResponse;
import com.mamehub.thrift.FileResponseCode;
import com.mamehub.thrift.PeerFileInfo;

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
