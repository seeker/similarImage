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
package com.github.dozedoff.similarImage.app;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.component.CoreComponent;
import com.github.dozedoff.similarImage.component.DaggerCoreComponent;
import com.github.dozedoff.similarImage.component.DaggerGuiComponent;
import com.github.dozedoff.similarImage.component.DaggerMessagingComponent;
import com.github.dozedoff.similarImage.component.GuiComponent;
import com.github.dozedoff.similarImage.component.MessagingComponent;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.github.dozedoff.similarImage.gui.SimilarImageController;
import com.github.dozedoff.similarImage.gui.SimilarImageView;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisEmbeddedServer;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;
import com.github.dozedoff.similarImage.messaging.HasherNode;
import com.github.dozedoff.similarImage.messaging.Node;
import com.github.dozedoff.similarImage.messaging.ResizerNode;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class SimilarImage {
	private static final int COLLECTED_MESSAGE_THRESHOLD = 100;
	/**
	 * Time in milliseconds
	 */
	private static final long COLLECTED_MESSAGE_DRAIN_INTERVAL = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

	private final static Logger logger = LoggerFactory.getLogger(SimilarImage.class);

	private static final String PROPERTIES_FILENAME = "similarImage.properties";
	private static final int PRODUCER_QUEUE_SIZE = 400;
	private static final int IMAGE_SIZE = 32;

	private Statistics statistics;

	private ArtemisEmbeddedServer aes;

	private MetricRegistry metrics;
	private Slf4jReporter reporter;

	private List<Node> nodes = new LinkedList<Node>();

	/**
	 * Parses the command line arguments for this program.
	 * 
	 * @param args
	 *            from the command line
	 * @return the parsed results
	 * @throws ArgumentParserException
	 *             if there is an error parsing the arguments
	 */
	protected static Namespace parseArgs(String[] args) throws ArgumentParserException {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("SimilarImage GUI").defaultHelp(true)
				.description("A similar image finder");
		parser.addArgument("--no-workers").help("Do not create any hash or resize nodes")
				.action(Arguments.storeTrue());

		return parser.parseArgs(args);
	}

	/**
	 * Check the parsed arguments if the workers are disabled.
	 * 
	 * @param args
	 *            parsed by {@link ArgumentParsers}
	 * @return true if no worker mode is enabled, else false
	 */
	protected static boolean isNoWorkersMode(Namespace args) {
		return args.getBoolean("no_workers");
	}

	/**
	 * Start the program
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		try {
			Namespace parsedArgs = parseArgs(args);

			new SimilarImage().init(isNoWorkersMode(parsedArgs));
		} catch (Exception e) {
			logger.error("Startup failed: {}", e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the program.
	 * 
	 * @param noWorkers
	 *            if local nodes should be created
	 * @throws Exception
	 *             if there is an error during setup
	 */
	public void init(boolean noWorkers) throws Exception {
		String version = this.getClass().getPackage().getImplementationVersion();

		if (version == null) {
			version = "unknown";
		}

		logger.info("SimilarImage version " + version);
		logger.info("System has {} processors", Runtime.getRuntime().availableProcessors());

		Settings settings = new Settings(new SettingsValidator());
		settings.loadPropertiesFromFile(PROPERTIES_FILENAME);

		CoreComponent coreComponent = DaggerCoreComponent.create();
		MessagingComponent messagingComponent = DaggerMessagingComponent.builder().coreComponent(coreComponent).build();

		this.metrics = messagingComponent.getMetricRegistry();

		aes = messagingComponent.getServer();
		aes.start();

		RepositoryFactory repositoryFactory = coreComponent.getRepositoryFactory();
		ImageRepository imageRepository = coreComponent.getImageRepository();

		// TODO module for node setup?
		nodes.add(messagingComponent.getRepositoryNode());
		nodes.add(messagingComponent.getResultMessageSink());

		statistics = messagingComponent.getStatistics();
		ArtemisSession as = messagingComponent.getSessionModule();

		GuiComponent guiComponent = DaggerGuiComponent.builder().messagingComponent(messagingComponent).build();

		SimilarImageView gui = guiComponent.getSimilarImageView();
		SimilarImageController controller = guiComponent.getSimilarImageController();

		logger.info("Starting metrics reporter...");
		reporter = Slf4jReporter.forRegistry(metrics).outputTo(LoggerFactory.getLogger("similarImage.metrics"))
				.convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(1, TimeUnit.MINUTES);

		logImageReaders();

		if (!noWorkers) {
			for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
				nodes.add(new HasherNode(as.getSession(), new ImagePHash(), QueueAddress.HASH_REQUEST.toString(),
						QueueAddress.RESULT.toString(), metrics));
				nodes.add(new ResizerNode(as.getSession(), new ImageResizer(IMAGE_SIZE), metrics));
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					logger.info("Stopping worker nodes...");

					nodes.forEach(node -> {
						node.stop();
					});

					logger.info("All worker nodes have terminated");
				}
			});
		}
	}

	private void logImageReaders() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
		logger.debug("Loaded JPEG readers:");
		while (readers.hasNext()) {
			logger.debug("reader: " + readers.next());
		}
	}
}
