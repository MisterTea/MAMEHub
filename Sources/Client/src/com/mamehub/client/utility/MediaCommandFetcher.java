package com.mamehub.client.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;

public class MediaCommandFetcher {
	final Logger logger = LoggerFactory.getLogger(MediaCommandFetcher.class);

	ConcurrentMap<String, TreeMap<String, ArrayList<String>>> media;
	
	public MediaCommandFetcher(URL url) throws IOException {
		media = Utils.getApplicationDatabaseEngine().getOrCreatePrimitiveMap("MediaCommands");
		media.clear();
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		
		reader.readLine();
		reader.readLine();
		
		String currentSystem = null;
		while(true) {
			String line = reader.readLine();
			if(line == null) {
				break;
			}
			//System.out.println(line);
			
			while(line.contains("  ")) {
				line = line.replace("  ", " ");
			}
			String[] tokens = line.split(" ");
			if(tokens.length < 3) {
				continue;
			}
			if(!tokens[0].equals("")) {
				// New system
				currentSystem = tokens[0];
				media.put(currentSystem, new TreeMap<String, ArrayList<String>>());
			}
			
			String mediaName = tokens[2].substring(1, tokens[2].length()-1);
			ArrayList<String> extensions = new ArrayList<String>();
			for(int i=3;i<tokens.length;i++) {
				//logger.info("ADDING EXTENSION FOR SYSTEM: " + currentSystem + " (" + mediaName + "): " + tokens[i]);
				extensions.add(tokens[i]);
			}
			
			if(media.get(currentSystem).containsKey(mediaName)) {
				media.get(currentSystem).get(mediaName).addAll(extensions);
			} else {
				media.get(currentSystem).put(mediaName, extensions);
			}
		}
	}
	
	public String getMediaName(String system, String cartFileName) {
		if(cartFileName.endsWith(".zip")) {
			// We need to get the actual cart file name inside the zip
			cartFileName = getCartFileName(cartFileName);
		}
		
		logger.info("GETTING MEDIA FOR " + cartFileName);
		for(Map.Entry<String,ArrayList<String>> entry : media.get(system).entrySet()) {
			for(String extension : entry.getValue()) {
				if(cartFileName.endsWith(extension)) {
					logger.info("GOT EXTENSION " + extension);
					logger.info("GOT MEDIA " + entry.getKey());
					return entry.getKey();
				}
			}
		}
		
		return "cartridge";
	}
	
	private String getCartFileName(String zipName) {
		ZipFile zf;
		try {
			zf = new ZipFile(zipName);
		} catch (IOException e1) {
			return null;
		}
		for (Enumeration<? extends ZipEntry> e = zf.entries(); e
				.hasMoreElements();) {
			ZipEntry ze = e.nextElement();
			String name = ze.getName();
			return name;
		}
		return null;
	}
}
