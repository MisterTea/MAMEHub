package com.mamehub.client.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.mamehub.client.Utils;
import com.mamehub.thrift.FileNameLocationPair;
import com.mamehub.thrift.MR;
import com.mamehub.thrift.RomInfo;
import com.mamehub.thrift.SoftwareList;

public class CfgReader extends DefaultHandler {
	public Map<String,String> inputs = new HashMap<String,String>();
	private boolean nextDataIsSequence;
	private String portName;
	private String sequence;
	
	public CfgReader(File file) throws IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
			saxParser.parse(new FileInputStream(file),
					this);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		nextDataIsSequence = false;

		if (qName.equals("newseq")) {
			nextDataIsSequence = true;
			sequence = "";
		} else if (qName.equals("port")) {
			portName = attributes.getValue("type");
		}
		// logger.info("Start Element :" + qName);
	}

	@Override
	public void characters(char ch[], int start, int length)
			throws SAXException {

		if (nextDataIsSequence) {
			sequence += new String(ch,start,length);
		}

		// logger.info("Characters: " + new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (qName.equals("port")) {
			inputs.put(portName, sequence.trim());
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		CfgReader reader = new CfgReader(new File("../cfg/default.cfg"));
		System.out.println(reader.inputs);
	}

}
