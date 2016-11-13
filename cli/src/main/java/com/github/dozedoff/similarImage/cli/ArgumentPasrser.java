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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;
import com.github.dozedoff.similarImage.messaging.HasherNode;
import com.github.dozedoff.similarImage.messaging.ResizerNode;

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
	private static final int DEFAULT_WINDOW = 1024 * 1024;

	private final List<HasherNode> hashWorkers = new LinkedList<HasherNode>();
	private final List<ResizerNode> resizeWorkers = new LinkedList<ResizerNode>();

	private enum CommandLineOptions {
		path, update, progress
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
		localSubcommand.addArgument("--" + enumToString(CommandLineOptions.progress)).action(Arguments.storeTrue())
				.help("Check the hashing progress of the given paths");

		int processors = Runtime.getRuntime().availableProcessors();
		Subparser nodeSubcommand = parser.addSubparsers().addParser("node").setDefault("subcommand", Subcommand.node);
		nodeSubcommand.addArgument("--port").type(Integer.class).setDefault(DEFAULT_ARTEMIS_CORE_PORT);
		nodeSubcommand.addArgument("--ip").type(String.class).setDefault(DEFAULT_IP);
		nodeSubcommand.addArgument("--resize").action(Arguments.storeTrue());
		nodeSubcommand.addArgument("--hash").action(Arguments.storeTrue());
		nodeSubcommand.addArgument("--resize-workers").help("Number of resize workers to start").type(Integer.class).setDefault(processors);
		nodeSubcommand.addArgument("--hash-workers").help("Number of hash workers to start").type(Integer.class).setDefault(processors);
		nodeSubcommand.addArgument("--status").action(Arguments.storeTrue());
		nodeSubcommand.addArgument("--window").help("Consumer window size in bytes").type(Integer.class).setDefault(DEFAULT_WINDOW);
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
		List<Object> paths = parsedArgs.getList(enumToString(CommandLineOptions.path));
		if (parsedArgs.getBoolean(enumToString(CommandLineOptions.update))) {
			walkPathsWithVisitor(paths, visitor);
		} else if (parsedArgs.getBoolean(enumToString(CommandLineOptions.progress))) {
			LOGGER.info("Checking progress...");
			Statistics statistics = new Statistics();
			walkPathsWithVisitor(paths,
					new ProgressVisitor(statistics, new HashAttribute(HashNames.DEFAULT_DCT_HASH_2)));
			outputProgress(statistics);
		}
	}

	private void walkPathsWithVisitor(List<Object> paths, FileVisitor<Path> pathVisitor) {
		for (Object path : paths) {
			try {
				Files.walkFileTree(Paths.get((String) path), pathVisitor);
			} catch (IOException e) {
				LOGGER.error("Failed to walk {}: {}", e.toString());
			}
		}
	}

	private void outputProgress(Statistics statistics) {
		ProgressCalc pc = new ProgressCalc(statistics);
		System.out.println(pc.toString());
	}

	private void nodeCommand(Namespace parsedArgs) {
		LOGGER.info("Connecting to {}:{}", parsedArgs.getString("ip"), parsedArgs.getInt("port"));

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("host", parsedArgs.getString("ip"));
		params.put("port", parsedArgs.getInt("port"));
		params.put(TransportConstants.SSL_ENABLED_PROP_NAME, "true");

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(NettyConnectorFactory.class.getName(), params))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD)
				.setBlockOnNonDurableSend(false).setBlockOnDurableSend(false).setPreAcknowledge(true)
				.setReconnectAttempts(3).setConsumerWindowSize(parsedArgs.getInt("window"));
		
		try (ArtemisSession session = new ArtemisSession(locator);) {
			if (parsedArgs.getBoolean("resize")) {
				startResizeWorkers(session, parsedArgs.getInt("resize_workers"));
			}

			if (parsedArgs.getBoolean("hash")) {
				startHashWorkers(session, parsedArgs.getInt("hash_workers"));
			}

			if (parsedArgs.getBoolean("status")) {
				logQueueSizes(session);
			}

			if (resizeWorkers.isEmpty() && hashWorkers.isEmpty()) {
				LOGGER.error("Failed to create any consumers, shutting down...");
				throw new RuntimeException("No consumers created");
			}

			Runtime.getRuntime().addShutdownHook(new CleanupWorkersOnShutdown(hashWorkers, resizeWorkers));

			try {
				// FIXME ugly, but it works...
				while (true) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				LOGGER.debug("Interrupted!");
			}
		} catch (Exception e) {
			LOGGER.error("Failed to start node: {}", e.toString());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("", e);
			}
		}
	}

	private void startHashWorkers(ArtemisSession session, int workerCount) {
		for (int i = 0; i < workerCount; i++) {
			LOGGER.info("Starting hash worker {} ...", i);
			try {
				HasherNode consumer = new HasherNode(session.getSession(), new ImagePHash(),
						ArtemisQueue.QueueAddress.HASH_REQUEST.toString(), ArtemisQueue.QueueAddress.RESULT.toString());
				hashWorkers.add(consumer);
			} catch (ActiveMQException e) {
				LOGGER.warn("Failed to create hash consumer: {} cause:", e.toString(), e.getCause().getMessage());
			}
		}
	}

	private void startResizeWorkers(ArtemisSession session, int workerCount) {
		for (int i = 0; i < workerCount; i++) {
			LOGGER.info("Starting resize worker {} ...", i);
			try {
				ResizerNode arrc = new ResizerNode(session.getSession(), new ImageResizer(32));
				resizeWorkers.add(arrc);
			} catch (Exception e) {
				LOGGER.warn("Failed to create resize consumer: {} cause:", e.toString(),
						e.getCause() == null ? null : e.getCause().toString());
			}
		}
	}

	private void logQueueSizes(ArtemisSession aSession) {
		try (ClientSession session = aSession.getSession()) {
			for (QueueAddress qa : QueueAddress.values()) {
				try {
					long queueSize = getQueueSize(session, qa);
					LOGGER.info("Queue {} size: {}", qa.toString(), queueSize);
				} catch (ActiveMQException e) {
					LOGGER.warn("Failed to get queue size for {}: {}", qa.toString(), e.toString());
				}
			}
		} catch (ActiveMQException e1) {
			LOGGER.error("Failed to get session: {}", e1.toString());
		}
	}

	private long getQueueSize(ClientSession session, QueueAddress address) throws ActiveMQException {
		return session.queueQuery(new SimpleString(address.toString())).getMessageCount();
	}

	private class CleanupWorkersOnShutdown extends Thread {
		private final List<HasherNode> hashWorkers;
		private final List<ResizerNode> resizeWorkers;

		public CleanupWorkersOnShutdown(List<HasherNode> hashWorkers, List<ResizerNode> resizeWorker) {
			super("Shutdown Hook");
			this.hashWorkers = hashWorkers;
			this.resizeWorkers = resizeWorker;
		}

		@Override
		public void run() {
			LOGGER.info("Shutting down...");

			for (HasherNode worker : this.hashWorkers) {
				worker.stop();
			}

			for (ResizerNode worker : this.resizeWorkers) {
				worker.stop();
			}
		}
	}
}
