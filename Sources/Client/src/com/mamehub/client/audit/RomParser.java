package com.mamehub.client.audit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mamehub.client.Utils;
import com.mamehub.thrift.RomHashEntryValue;
import com.mamehub.thrift.MR;
import com.mamehub.thrift.RomInfo;
import com.mamehub.thrift.SoftwareList;

public class RomParser extends DefaultHandler {
	final Logger logger = LoggerFactory.getLogger(RomParser.class);

	private String xmlFile;
	private RomInfo romInfo;
	private boolean nextDataIsDescription;
	private boolean gameFailed;

	private int goodRoms;
	private int romsWithNoHash;
	private Set<String> possibleEntries;

	private Map<String, ArrayList<RomHashEntryValue>> hashEntryMap;
	private ConcurrentMap<String, RomInfo> roms;
	private boolean allowZeroFileRoms;
	private ConcurrentMap<String, String> chdMap;
	private boolean matchFileName;

	private boolean verbose = false;
	private int count = 0;

	private boolean mess;

	private List<RomInfo> systems;

	private boolean chdFailed;

	public RomParser(String xmlFile) {
		this.xmlFile = xmlFile;
	}

	void process(Map<String, ArrayList<RomHashEntryValue>> hashEntryMap2,
			ConcurrentMap<String, String> chdMap,
			ConcurrentMap<String, RomInfo> roms, boolean allowZeroFileRoms,
			boolean matchFileName, boolean mess) throws IOException {
		this.roms = roms;
		this.matchFileName = matchFileName;
		this.hashEntryMap = hashEntryMap2;
		this.chdMap = chdMap;
		this.allowZeroFileRoms = allowZeroFileRoms;
		this.mess = mess;
		systems = new ArrayList<RomInfo>();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
			saxParser.parse(new GZIPInputStream(new FileInputStream(xmlFile)),
					this);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}
		Utils.getAuditDatabaseEngine().commit();

		// I used to fail the rom if the clone/parent fails, but that isn't
		// always true.
		/*
		 * for (String romId : new HashSet<String>(roms.keySet())) { RomInfo
		 * romInfo = new RomInfo(roms.get(romId)); if (romInfo.cloneRom != null
		 * && roms.get(romInfo.cloneRom).missingReason != null) {
		 * romInfo.missingReason = MR.MISSING_CLONE; } else if
		 * (romInfo.parentRom != null &&
		 * roms.get(romInfo.parentRom).missingReason != null) {
		 * romInfo.missingReason = MR.MISSING_PARENT; } roms.put(romId,
		 * romInfo); }
		 */
		this.roms = null;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		nextDataIsDescription = false;

		if (qName.equals("game") || qName.equals("machine")) {
			romInfo = new RomInfo();
			romInfo.parentRom = attributes.getValue("romof");
			romInfo.cloneRom = attributes.getValue("cloneof");
			romInfo.id = attributes.getValue("name");
			verbose = romInfo.id.equals("alien");
			gameFailed = false;
			chdFailed = false;
			romsWithNoHash = goodRoms = 0;
			possibleEntries = null;
		} else if (qName.equals("description")) {
			nextDataIsDescription = true;
		} else if (qName.equals("disk")) {
			if (attributes.getValue("sha1") == null) {
				chdFailed = true;
			} else {
				if (chdMap != null
						&& chdMap.containsKey(attributes.getValue("sha1"))) {
					romInfo.chdFilenames.add(chdMap.get(attributes.getValue("sha1")));
				} else {
					//System.out.println("MISSING CHD: " + attributes.getValue("name") + " " + attributes.getValue("sha1"));
					chdFailed = true;
				}
			}
		} else if (qName.equals("softwarelist")) {
			romInfo.softwareLists.add(new SoftwareList().setName(
					attributes.getValue("name")).setFilter(
					attributes.getValue("filter")));
		} else if (qName.equals("rom")) {
			if (attributes.getValue("merge") != null) {
				return; // Skip roms that should be part of a parent/clone
			}

			if (gameFailed) {
				return; // Already failed, no point in continuing
			}

			/*
			 * String sha1 = attributes.getValue("sha1");
			 * 
			 * if(sha1 == null) { romsWithNoHash++; return; }
			 * 
			 * Set<String> entries = hashEntryMap.get(sha1);
			 */

			String crc32 = attributes.getValue("crc");

			if (crc32 == null) {
				romsWithNoHash++;
				return;
			}

			String name = attributes.getValue("name");

			List<RomHashEntryValue> entries = hashEntryMap.get(crc32);

			if (entries == null) {
				gameFailed = true;
				return;
			}

			if (verbose) {
				logger.info(matchFileName + " GOT " + entries.size() + " FOR "
						+ romInfo.id + " : " + name);
			}

			if (matchFileName) {
				for (RomHashEntryValue f : entries) {
					if (verbose) {
						logger.info("" + f.location + " "
								+ romInfo.id.toLowerCase());
					}
					if (new File(f.location).getName().toLowerCase()
							.startsWith(romInfo.id.toLowerCase())) {
						if (possibleEntries == null) {
							possibleEntries = new HashSet<String>();
						}
						possibleEntries.add(f.location);
						if (verbose) {
							logger.info("ADDING POSSIBLE LOCATION");
						}
					}
				}
			} else {
				Set<String> newEntries = new HashSet<String>();
				for (RomHashEntryValue f : entries) {
					if (f.system == null) {
						// Do not add any MESS software as roms. Software is
						// processed by the cart parser.
						newEntries.add(f.location);
					}
				}
				if (possibleEntries == null) {
					possibleEntries = new HashSet<String>(newEntries);
				} else {
					possibleEntries.retainAll(newEntries);
				}
			}

			goodRoms++;

		}
		// logger.info("Start Element :" + qName);
	}

	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {

		if (nextDataIsDescription) {
			if (romInfo.description == null) {
				romInfo.description = "";
			}
			romInfo.description = romInfo.description.concat(new String(ch,
					start, length));
		}

		// logger.info("Characters: " + new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals("game") || qName.equals("machine")) {
			if (gameFailed) {
				romInfo.missingReason = MR.MISSING_FILES;
			} else if (romsWithNoHash == 0 && goodRoms == 0
					&& allowZeroFileRoms) {
				// This is ok
			} else if ((romsWithNoHash > 0 && goodRoms == 0)
					|| (goodRoms > 0 && possibleEntries == null)
					|| (goodRoms > 0 && possibleEntries.isEmpty())) {
				if (verbose) {
					logger.info("BAD FILES: " + possibleEntries);
				}
				romInfo.missingReason = MR.BAD_FILES;
			} else {
				if (possibleEntries != null && !possibleEntries.isEmpty()) {
					if (!possibleEntries.isEmpty()) {
						romInfo.filenames.add(possibleEntries.iterator().next());
					} else {
						romInfo.missingReason = MR.MISSING_FILES;
					}
				} else {
					romInfo.missingReason = MR.MISSING_FILES;
				}
			}
			if (!mess) {
				romInfo.system = "Arcade";
			} else {
				romInfo.system = "Bios";
			}
			if (verbose)
				logger.info("ROM INFO: " + romInfo);
			if (chdFailed) {
				romInfo.missingReason = MR.MISSING_CHD;
			}
			roms.put(romInfo.id, romInfo);
			count++;
			if (count % 5000 == 0) {
				Utils.getAuditDatabaseEngine().commit();
			}
			if (!romInfo.softwareLists.isEmpty()) {
				systems.add(romInfo);
			}
		}
	}

	public List<RomInfo> getSystems() {
		return systems;
	}
}
