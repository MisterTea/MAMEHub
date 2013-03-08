package com.mamehub.client.audit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mamehub.client.Utils;

public class IniParser {
	final Logger logger = LoggerFactory.getLogger(IniParser.class);

	private File iniFile;

	public IniParser(File iniFile) {
		this.iniFile = iniFile;
	}
	
	public List<File> getRomPaths() {
		try {
			String iniData = Utils.fileToString(iniFile);
			
			int rompathIndex = iniData.indexOf("rompath");
			
			iniData = iniData.substring(rompathIndex + "rompath".length()); // Start after 'rompath'
			iniData = iniData.substring(0, iniData.indexOf("\n")); // End at end of line
			iniData = iniData.trim();
			
			ArrayList<File> files = new ArrayList<File>();
			String[] directories = iniData.split(";");
			for(String directory : directories) {
				File file = new File(directory);
				if(file.exists())
					files.add(file);
			}
			return files;
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<File>();
		}
	}
	
	public void setRomPaths(List<File> paths) {
		try {
			String iniData = Utils.fileToString(iniFile);
			
			int rompathIndex = iniData.indexOf("rompath");
			
			String iniString = iniData.substring(rompathIndex + "rompath".length()); // Start after 'rompath'
			iniString = iniString.substring(0, iniString.indexOf("\n")).trim(); // End at end of line
			logger.info("INI STRING: " + iniString);
			
			String newIniString = "";
			boolean first=true;
			for(File f : paths) {
				if(first) {
					first=false;
				} else {
					newIniString += ";";
				}
				
				newIniString += f.getPath();
			}
			logger.info("NEW INI STRING: " + newIniString);
			iniData = iniData.replace(iniString, newIniString);
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(iniFile));
			bw.write(iniData);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addRomPath(File file) {
		List<File> files = getRomPaths();
		files.add(file);
		setRomPaths(files);
	}

	public void removeRomPath(File f) {
		List<File> files = getRomPaths();
		files.remove(f);
		setRomPaths(files);
	}
}
