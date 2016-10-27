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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.github.dozedoff.similarImage.messaging.RepositoryNode;
import com.github.dozedoff.similarImage.messaging.ResizerNode;
import com.github.dozedoff.similarImage.thread.NamedThreadFactory;
import com.github.dozedoff.similarImage.thread.SorterFactory;
import com.j256.ormlite.dao.DaoManager;

public class SimilarImage {
	private final static Logger logger = LoggerFactory.getLogger(SimilarImage.class);

	private final String PROPERTIES_FILENAME = "similarImage.properties";
	private final int PRODUCER_QUEUE_SIZE = 400;
	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024;
	private static final int RESIZE_SIZE = 32;

	private ExecutorService threadPool;
	private Statistics statistics;
	RepositoryNode rn;
	List<HasherNode> ahrcs = new LinkedList<>();
	List<ResizerNode> arrcs = new LinkedList<>();

	ArtemisEmbeddedServer aes;

	public static void main(String[] args) {
		try {
			new SimilarImage().init();
		} catch (Exception e) {
			logger.error("Startup failed: {}", e.toString());
			e.printStackTrace();
		}
	}

	public void init() throws Exception {
		String version = this.getClass().getPackage().getImplementationVersion();

		if (version == null) {
			version = "unknown";
		}

		logger.info("SimilarImage version " + version);
		logger.info("System has {} processors", Runtime.getRuntime().availableProcessors());

		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
				new NamedThreadFactory(SimilarImage.class.getSimpleName()));
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

		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			ahrcs.add(new HasherNode(as.getSession(), new ImagePHash(), QueueAddress.HASH_REQUEST.toString(),
					QueueAddress.RESULT.toString()));
			arrcs.add(new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE)));
		}

		RepositoryFactory repositoryFactory = new OrmliteRepositoryFactory(database);

		ImageRepository imageRepository = repositoryFactory.buildImageRepository();
		FilterRepository filterRepository = repositoryFactory.buildFilterRepository();
		TagRepository tagRepository = repositoryFactory.buildTagRepository();

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		rn = new RepositoryNode(as.getSession(), pendingRepo, imageRepository);

		DuplicateOperations dupOps = new DuplicateOperations(filterRepository, tagRepository, imageRepository);
		SorterFactory sf = new SorterFactory(imageRepository, filterRepository, tagRepository);

		statistics = new Statistics();
		HandlerListFactory hlf = new HandlerListFactory(imageRepository, statistics, as, eaQuery);
		UserTagSettingController utsc = new UserTagSettingController(tagRepository);

		DisplayGroupView dgv = new DisplayGroupView();
		SimilarImageController controller = new SimilarImageController(sf, hlf, dupOps, dgv, statistics, utsc);
		SimilarImageView gui = new SimilarImageView(controller, dupOps, PRODUCER_QUEUE_SIZE, utsc, filterRepository);

		controller.setGui(gui);

		logImageReaders();
	}

	private void logImageReaders() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("JPEG");
		logger.debug("Loaded JPEG readers:");
		while (readers.hasNext()) {
			logger.debug("reader: " + readers.next());
		}
	}
}
