/*  Copyright (C) 2016  Nicholas Wright
    
    This file is part of similarImage - A similar image finder using pHash
    
    similarImage is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.dozedoff.similarImage.cli;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.messaging.ArtemisHashConsumer;
import com.github.dozedoff.similarImage.messaging.ArtemisQueueAddress;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Parses command line parameters and executes the corresponding tasks.
 * 
 * @author Nicholas Wright
 */
public class ArgumentPasrser {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentPasrser.class);
	
	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024;
	private static final int DEFAULT_ARTEMIS_CORE_PORT = 61616;
	private static final String DEFAULT_IP = "127.0.0.1";

	private enum CommandLineOptions {
		path, update,
	};

	private enum Subcommand {
		node, local
	}

	private final FileVisitor<Path> visitor;
	private final ArgumentParser parser;

	private static String enumToString(CommandLineOptions optionEnum) {
		return optionEnum.toString();
	}

	/**
	 * Setup the command line parser.
	 * 
	 * @param visitor
	 *            visitor to use processing files
	 */
	public ArgumentPasrser(FileVisitor<Path> visitor) {
		this.visitor = visitor;

		// TODO add hash selection

		parser = ArgumentParsers.newArgumentParser("SimilarImage CLI").defaultHelp(true)
				.description("Limited set of SimilarImage functions");

		Subparser localSubcommand = parser.addSubparsers().addParser("local").setDefault("subcommand",
				Subcommand.local);

		localSubcommand.addArgument("--" + enumToString(CommandLineOptions.update)).action(Arguments.storeTrue())
				.help("Check extended attributes and update if missing or invalid");
		localSubcommand.addArgument(enumToString(CommandLineOptions.path)).metavar("P").nargs("*").type(String.class)
				.help("Process all files in the given directory");

		Subparser nodeSubcommand = parser.addSubparsers().addParser("node").setDefault("subcommand", Subcommand.node);
		nodeSubcommand.addArgument("--port").type(Integer.class).setDefault(DEFAULT_ARTEMIS_CORE_PORT);
		nodeSubcommand.addArgument("--ip").type(String.class).setDefault(DEFAULT_IP);
	}

	/**
	 * Parse command line arguments and execute tasks accordingly.
	 * 
	 * @param args
	 *            command line parameters
	 * @throws ArgumentParserException
	 *             if there is an error parsing command line arguments
	 */
	public void parseArgs(String[] args) throws ArgumentParserException {
		Namespace parsedArgs = parser.parseArgs(args);

		if (parsedArgs.get("subcommand") == Subcommand.local) {
			localCommand(parsedArgs);
		}

		if (parsedArgs.get("subcommand") == Subcommand.node) {
			nodeCommand(parsedArgs);
		}
	}

	private void localCommand(Namespace parsedArgs) {
		if (parsedArgs.getBoolean(enumToString(CommandLineOptions.update))) {
			for (Object path : parsedArgs.getList(enumToString(CommandLineOptions.path))) {
				try {
					Files.walkFileTree(Paths.get((String) path), visitor);
				} catch (IOException e) {
					LOGGER.error("Failed to walk {}: {}", e.toString());
				}
			}
		}
	}

	private void nodeCommand(Namespace parsedArgs) {
		LOGGER.info("I am a node! port: {}, ip: {}", parsedArgs.getInt("port"), parsedArgs.getString("ip"));

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("host", parsedArgs.getString("ip"));
		params.put("port", parsedArgs.getInt("port"));

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName(), params))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD)
				.setBlockOnNonDurableSend(false).setPreAcknowledge(true);

		try {
			ArtemisSession session = new ArtemisSession(locator);
			new ArtemisHashConsumer(session.getSession(), new ImagePHash(), ArtemisQueueAddress.hash.toString(),
					ArtemisQueueAddress.result.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
