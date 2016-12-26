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

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmlitePendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteRepositoryFactory;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.github.dozedoff.similarImage.duplicate.DuplicateOperations;
import com.github.dozedoff.similarImage.gui.DisplayGroupView;
import com.github.dozedoff.similarImage.gui.SimilarImageController;
import com.github.dozedoff.similarImage.gui.SimilarImageView;
import com.github.dozedoff.similarImage.gui.UserTagSettingController;
import com.github.dozedoff.similarImage.handler.HandlerListFactory;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.Statistics;
import com.github.dozedoff.similarImage.messaging.ArtemisEmbeddedServer;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.ArtemisSession;
import com.github.dozedoff.similarImage.messaging.HasherNode;
import com.github.dozedoff.similarImage.messaging.MessageCollector;
import com.github.dozedoff.similarImage.messaging.Node;
import com.github.dozedoff.similarImage.messaging.QueueToDatabaseTransaction;
import com.github.dozedoff.similarImage.messaging.RepositoryNode;
import com.github.dozedoff.similarImage.messaging.ResizerNode;
import com.github.dozedoff.similarImage.messaging.ResultMessageSink;
import com.github.dozedoff.similarImage.messaging.TaskMessageHandler;
import com.github.dozedoff.similarImage.thread.SorterFactory;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;

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
	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024 * 100;
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
		this.metrics = new MetricRegistry();
		String version = this.getClass().getPackage().getImplementationVersion();

		if (version == null) {
			version = "unknown";
		}

		logger.info("SimilarImage version " + version);
		logger.info("System has {} processors", Runtime.getRuntime().availableProcessors());

		Settings settings = new Settings(new SettingsValidator());
		settings.loadPropertiesFromFile(PROPERTIES_FILENAME);

		aes = new ArtemisEmbeddedServer();
		aes.start();

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD)
				.setBlockOnNonDurableSend(false).setBlockOnDurableSend(false).setPreAcknowledge(true);

		ArtemisSession as = new ArtemisSession(locator);

		Database database = new SQLiteDatabase();

		PendingHashImageRepository pendingRepo = new OrmlitePendingHashImage(
				DaoManager.createDao(database.getCs(), PendingHashImage.class));

		RepositoryFactory repositoryFactory = new OrmliteRepositoryFactory(database);

		ImageRepository imageRepository = repositoryFactory.buildImageRepository();

		TaskMessageHandler tmh = new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics);
		nodes.add(new RepositoryNode(as.getSession(), pendingRepo, tmh, metrics));

		TransactionManager tm = new TransactionManager(database.getCs());
		QueueToDatabaseTransaction qdt = new QueueToDatabaseTransaction(as.getTransactedSession(), tm, pendingRepo,
				imageRepository, metrics);

		MessageCollector mc = new MessageCollector(COLLECTED_MESSAGE_THRESHOLD, qdt);
		ResultMessageSink sink = new ResultMessageSink(as.getTransactedSession(), mc, QueueAddress.RESULT.toString(),
				COLLECTED_MESSAGE_DRAIN_INTERVAL);
		nodes.add(sink);

		FilterRepository filterRepository = repositoryFactory.buildFilterRepository();
		TagRepository tagRepository = repositoryFactory.buildTagRepository();

		DuplicateOperations dupOps = new DuplicateOperations(filterRepository, tagRepository, imageRepository);
		SorterFactory sf = new SorterFactory(imageRepository, filterRepository, tagRepository);

		statistics = new Statistics();
		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		HandlerListFactory hlf = new HandlerListFactory(imageRepository, statistics, as, eaQuery);
		UserTagSettingController utsc = new UserTagSettingController(tagRepository);

		DisplayGroupView dgv = new DisplayGroupView();
		SimilarImageController controller = new SimilarImageController(sf, hlf, dupOps, dgv, statistics, utsc);
		SimilarImageView gui = new SimilarImageView(controller, dupOps, PRODUCER_QUEUE_SIZE, utsc, filterRepository);

		controller.setGui(gui);

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
