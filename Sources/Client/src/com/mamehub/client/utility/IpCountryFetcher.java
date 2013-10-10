package com.mamehub.client.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.mamehub.thrift.IpRangeData;

public class IpCountryFetcher {
	private List<IpRangeData> ranges = new ArrayList<IpRangeData>();
	
	public IpCountryFetcher(URL url) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		
		ranges.clear();
		String line;
		while(true) {
			line = reader.readLine();
			if(line == null) {
				break;
			}
			if(line.charAt(0)=='#') {
				continue;
			}
			String[] tokens = line.replace("\"", "").split(",");
			
			ranges.add(new IpRangeData(Long.parseLong(tokens[0]), Long.parseLong(tokens[1]),
					tokens[4], tokens[5], tokens[6]));
		}
	}
	
	public IpRangeData getRangeData(String ipAddress) {
		long binaryAddress = ipToBinary(ipAddress);
		
		for(IpRangeData range : ranges) {
			if(range.ipStart<=binaryAddress && range.ipEnd>=binaryAddress) {
				return range;
			}
		}
		return null;
	}
	
	private long ipToBinary(String ipAddress) {
	    String[] octets = ipAddress.split("\\.");
	    return (Long.valueOf(octets[0])<<24)+(Long.valueOf(octets[1])<<16)+
	    		(Long.valueOf(octets[2])<<8)+(Long.valueOf(octets[3]));
	}
}
