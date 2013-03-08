package com.mamehub.client.utility;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.mamehub.client.net.RpcEngine;

public class CommandLineFlags {
	public static CommandLine cmd;
	public CommandLineFlags(String[] args) {
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption("hostname", false, "override server name");
		
		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse( options, args);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		// Deal with some globally-affecting command line flags here
		if(cmd.hasOption("hostname")) {
			RpcEngine.HOSTNAME = cmd.getOptionValue("hostname");
		}
	}
}
