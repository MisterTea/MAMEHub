package com.mamehub.client.audit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mamehub.client.Utils;
import com.mamehub.client.utility.ClientDatabaseEngine;
import com.mamehub.thrift.MR;
import com.mamehub.thrift.RomHashEntryValue;
import com.mamehub.thrift.RomInfo;

public class CartParser extends DefaultHandler implements Runnable {
	final Logger logger = LoggerFactory.getLogger(CartParser.class);

	private String systemName;
	private File xmlFile;

	private Map<String, ArrayList<RomHashEntryValue>> hashEntryMap;

	private Map<String, RomInfo> roms;

	private ConcurrentMap<String, String> chdMap;
	private int count = 0;

	private boolean debug;

	private boolean missingSystem;
	private boolean verbose=false;

	public CartParser(String systemName, File xmlFile,
			Map<String, ArrayList<RomHashEntryValue>> hashEntryMap,
			Map<String, RomInfo> roms, ConcurrentMap<String, String> chdMap,
			boolean missingSystem) {
		this.systemName = systemName;
		this.hashEntryMap = hashEntryMap;
		this.roms = roms;
		this.chdMap = chdMap;
		this.missingSystem = missingSystem;
		if (!systemName.equals(systemName.toLowerCase())) {
			throw new RuntimeException("System names must be lower case");
		}
		this.xmlFile = xmlFile;
		logger.info("Parsing carts for system: " + systemName + " ("
				+ xmlFile.getName() + ")");
	}

	@Override
	public void run() {
		try {
			if (systemName.contains(":")) {
				throw new RuntimeException("Colons not allowed in system names");
			}
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("software");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node node = nList.item(temp);
				
				RomInfo romInfo = new RomInfo();
                romInfo.id = node.getAttributes().getNamedItem("name").getTextContent();
				romInfo.description = ((Element)node).getElementsByTagName("description").item(0).getTextContent();
				romInfo.system = systemName;
				
				verbose = (romInfo.id.equals("10yard"));
				
				NodeList childList = ((Element)node).getElementsByTagName("part");
				if (verbose) {
					System.out.println("NUM PARTS: " + childList.getLength());
				}
				for (int t2=0;t2<childList.getLength();t2++) {
					Node childNode = childList.item(t2);
					
					String interfaceType = childNode.getAttributes().getNamedItem("name").getTextContent();
					String interfaceName = childNode.getAttributes().getNamedItem("interface").getTextContent().split("_")[0] + ":" + romInfo.id;
					romInfo.interfaceFileMap.put(interfaceType, interfaceName);
					
					NodeList diskAreaList = ((Element)childNode).getElementsByTagName("diskarea");
					for (int diskAreaI=0;diskAreaI<diskAreaList.getLength();diskAreaI++) {
						Node diskAreaNode = diskAreaList.item(diskAreaI);
						String chdSha1 = ((Element)diskAreaNode).getElementsByTagName("disk").item(0).getAttributes().getNamedItem("sha1").getTextContent();
						if (!chdMap.containsKey(chdSha1)) {
							romInfo.missingReason = MR.MISSING_CHD;
						}
					}
					
					NodeList dataAreaList = ((Element)childNode).getElementsByTagName("dataarea");
					if (verbose) {
						System.out.println("NUM DATA: " + dataAreaList.getLength());
					}
					for (int diskAreaI=0;diskAreaI<dataAreaList.getLength();diskAreaI++) {
						Node dataAreaNode = dataAreaList.item(diskAreaI);
						NodeList romNodeList = ((Element)dataAreaNode).getElementsByTagName("rom");
						for (int romI=0;romI<romNodeList.getLength();romI++) {
							Node romNode = romNodeList.item(romI);
							if (romNode.getAttributes().getNamedItem("name") != null &&
									romNode.getAttributes().getNamedItem("crc") != null) {
								String softwareName = romNode.getAttributes().getNamedItem("name").getTextContent();
								String romCrc = romNode.getAttributes().getNamedItem("crc").getTextContent();
								if (verbose) {
									System.out.println("ROM INFO: " + softwareName + "/" + romCrc);
								}
								List<RomHashEntryValue> hashEntries = hashEntryMap.get(romCrc);

								boolean gotRom=false;
								if (hashEntries != null) {
									for (RomHashEntryValue v : hashEntries) {
										if (verbose) {
											System.out.println("HASH INFO: " + v + " " + systemName);
										}
										if (v.system != null && v.system.equalsIgnoreCase(systemName)) {
											gotRom=true;
											break;
										}
									}
								}
								if (!gotRom) {
									romInfo.missingReason = MR.MISSING_FILES;
								}
							}
						}
					}
				}
				
				if (missingSystem && romInfo.missingReason == null) {
					romInfo.missingReason = MR.MISSING_SYSTEM;
				}
				if (verbose) {
					System.out.println("FINAL RESULT: " + romInfo);
				}
				roms.put(romInfo.id, romInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.hashEntryMap = null;
		this.roms = null;
	}
}
