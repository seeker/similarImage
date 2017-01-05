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
package com.github.dozedoff.similarImage.messaging;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.to;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.component.CoreComponent;
import com.github.dozedoff.similarImage.component.DaggerCoreComponent;
import com.github.dozedoff.similarImage.component.DaggerMessagingComponent;
import com.github.dozedoff.similarImage.component.MessagingComponent;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.module.ArtemisBrokerModule;
import com.github.dozedoff.similarImage.module.SQLitePersistenceModule;
import com.github.dozedoff.similarImage.util.TestUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

@RunWith(MockitoJUnitRunner.class)
public class MessagingIT {
	private static final int MESSAGE_DRAIN_INTERVAL = 500;
	private static List<HasherNode> ahrcs = new LinkedList<>();
	private static List<ResizerNode> arrcs = new LinkedList<>();

	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024;
	private static final int RESIZE_SIZE = 32;
	private static final Path TEST_PATH = Paths.get("foo");

	private static ArtemisEmbeddedServer aes;

	private static Database database;
	private ImageRepository imageRepository;
	private static ArtemisHashProducer ahp;
	private static ArtemisSession as;
	private PendingHashImageRepository pendingRepo;

	private static Path dbFile;
	private static Path workingdir;
	private static Path testImageAutumnOriginal;
	private Path testImageAutumn;
	private static Path testImageCorruptOriginal;
	private Path testImageCorrupt;
	private static long testImageAutumnReferenceHash;

	private Duration messageTimeout = new Duration(6, TimeUnit.SECONDS);
	private MessageCollector mc;
	private ResultMessageSink sink;

	private MetricRegistry metrics;

	ClientSession queueJanitor;

	private static CoreComponent coreComponent;
	private static MessagingComponent messageComponent;

	@BeforeClass
	public static void classSetup() throws Exception {
		workingdir = Files.createTempDirectory("MessageIntegration");
		dbFile = Files.createTempFile(workingdir, "database", ".db");

		testImageAutumnOriginal = Paths.get(Thread.currentThread().getContextClassLoader().getResource("images/autumn.jpg").toURI());
		testImageCorruptOriginal = Paths.get(Thread.currentThread().getContextClassLoader().getResource("images/corrupt.jpg").toURI());

		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testImageAutumnOriginal))) {
			testImageAutumnReferenceHash = new ImagePHash().getLongHash(bis);
		}
		

		coreComponent = DaggerCoreComponent.builder().sQLitePersistenceModule(new SQLitePersistenceModule(dbFile))
				.build();
		database = coreComponent.getDatabase();

		messageComponent = DaggerMessagingComponent.builder().coreComponent(coreComponent)
				.artemisBrokerModule(new ArtemisBrokerModule(workingdir)).build();

		aes = messageComponent.getServer();
		aes.start();

	}

	@AfterClass
	public static void classTearDown() throws Exception {
		aes.stop();
		TestUtil.deleteAllFiles(workingdir);
	}

	@Before
	public void setup() throws Exception {
		clearDatabase(database.getCs());
		testImageAutumn = Files.createTempFile(Paths.get(""), "testImage", ".jpg");
		testImageCorrupt = Files.createTempFile(Paths.get(""), "corrupt", ".jpg");

		Files.copy(testImageAutumnOriginal, testImageAutumn, StandardCopyOption.REPLACE_EXISTING);
		Files.copy(testImageCorruptOriginal, testImageCorrupt, StandardCopyOption.REPLACE_EXISTING);

		imageRepository = coreComponent.getImageRepository();
		pendingRepo = coreComponent.getPendingHashImageRepository();

		metrics = messageComponent.getMetricRegistry();

		as = messageComponent.getSessionModule();

		queueJanitor = as.getSession();

		for (String queue : new String[] { QueueAddress.RESULT.toString(), QueueAddress.EA_UPDATE.toString(),
				QueueAddress.RESULT.toString() }) {
			recreateQueue(queue);
		}

		sink = messageComponent.getResultMessageSink();

	}

	private void recreateQueue(String queueName) {
		try {
			queueJanitor.deleteQueue(queueName);
		} catch (ActiveMQException e) {
			System.err.println(e.toString());
		}

		try {
			queueJanitor.createQueue(queueName, queueName);
		} catch (ActiveMQException e) {
			System.err.println(e.toString());
		}
	}

	private void clearDatabase(ConnectionSource cs) throws SQLException {
		// TODO add empty interface to tables
		Class[] tables = {ImageRecord.class, PendingHashImage.class, Tag.class, Thumbnail.class};
		
		for(Class table : tables){
			TableUtils.clearTable(cs, table);
		}
		}

	@After
	public void tearDown() throws Exception {
		clearDatabase(database.getCs());

		Files.delete(testImageAutumn);
		Files.delete(testImageCorrupt);
		sink.stop();
		as.close();
	}

	@AfterClass
	public static void tearDownClass() throws IOException {
		database.close();
		Files.deleteIfExists(dbFile);
	}

	@Test
	public void testHashImage() throws Exception {
		String hashQueue = "hashImageHash";
		String resizeQueue = "hashImageResize";
		String resultQueue = QueueAddress.RESULT.toString();
		String queryQueue = "hashImageQuery";
		String eaQueue = "eaqueue";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);
		noDupe.createTemporaryQueue(eaQueue, eaQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, QueueAddress.RESULT.toString(),
				pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		new HasherNode(as.getSession(), new ImagePHash(), hashQueue, resultQueue, metrics);
		new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage, metrics);

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttributeDirectoryCache(new ExtendedAttribute()),
				new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), Collections.emptyList(), resizeQueue, eaQueue);
		ahp = new ArtemisHashProducer(sn);

		RepositoryNode rn = new RepositoryNode(noDupe, queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).untilCall(to(imageRepository).getByHash(testImageAutumnReferenceHash),
				containsInAnyOrder(new ImageRecord(testImageAutumn.toString(), testImageAutumnReferenceHash)));
	}

	@Test
	public void testDoNotQueueDuplicates() throws Exception {
		String resizeQueue = "dupeResize";
		String hashQueue = "dupeHash";
		String queryQueue = "dupeQuery";
		String eaQueue = "eaQuery";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);
		noDupe.createTemporaryQueue(eaQueue, eaQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, QueueAddress.RESULT.toString(),
				pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttributeDirectoryCache(new ExtendedAttribute()),
				new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), Collections.emptyList(), resizeQueue, eaQueue);
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);
		ResizerNode arrc = new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue,
				queryMessage, metrics);

		ClientConsumer checkConsumer = noDupe.createConsumer(hashQueue, true);

		ahp.handle(testImageAutumn);
		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).until(new Callable<Long>() {

			@Override
			public Long call() throws Exception {
				return noDupe.queueQuery(new SimpleString(hashQueue)).getMessageCount();
			}
		}, is(1L));

		noDupe.close();
	}

	@Test
	public void testPendingImagesQuery() throws Exception {
		String requestQueue = "pendingQueryRequest";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(requestQueue, requestQueue);

		pendingRepo.store(new PendingHashImage(testImageAutumn, UUID.randomUUID()));

		QueryMessage qm = new QueryMessage(noDupe, requestQueue);
		RepositoryNode qr = new RepositoryNode(as.getSession(), requestQueue, QueueAddress.RESULT.toString(),
				pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		await().atMost(messageTimeout).until(new Callable<List<String>>() {

			@Override
			public List<String> call() throws Exception {
				return qm.pendingImagePaths();
			}
		}, is(containsInAnyOrder(testImageAutumn.toString())));
	}

	@Test
	public void testEAupdated() throws Exception {
		String hashQueue = "eaHash";
		String resizeQueue = "eaResize";
		String resultQueue = QueueAddress.RESULT.toString();
		String queryQueue = "eaeaQuery";
		String eaQueue = QueueAddress.EA_UPDATE.toString();

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), eaQueue, metrics), metrics);

		new HasherNode(as.getSession(), new ImagePHash(), hashQueue, resultQueue, metrics);
		new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage, metrics);

		HashAttribute ha = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttribute(), ha, Collections.emptyList(), resizeQueue, eaQueue);
		ahp = new ArtemisHashProducer(sn);

		RepositoryNode rn = new RepositoryNode(noDupe, queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		assertThat(ha.areAttributesValid(testImageAutumn), is(false));// guard assert

		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).untilCall(to(ha).areAttributesValid(testImageAutumn), is(true));
	}

	@Test
	public void testMarkCorrupt() throws Exception {
		String hashQueue = "corrHash";
		String resizeQueue = "corrResize";
		String resultQueue = QueueAddress.RESULT.toString();
		String queryQueue = "correaQuery";
		String eaQueue = "EA_UPDATE";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), eaQueue, metrics), metrics);

		new HasherNode(as.getSession(), new ImagePHash(), hashQueue, resultQueue, metrics);
		new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage, metrics);

		HashAttribute ha = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttribute(), ha, Collections.emptyList(), resizeQueue, eaQueue);
		ahp = new ArtemisHashProducer(sn);

		RepositoryNode rn = new RepositoryNode(noDupe, queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), metrics), metrics);

		assertThat(ha.isCorrupted(testImageCorrupt), is(false));// guard assert

		ahp.handle(testImageCorrupt);

		await().atMost(messageTimeout).untilCall(to(ha).isCorrupted(testImageCorrupt), is(true));
	}
}
