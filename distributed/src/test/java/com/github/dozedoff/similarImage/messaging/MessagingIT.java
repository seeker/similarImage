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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmlitePendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteRepositoryFactory;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.util.TestUtil;
import com.j256.ormlite.dao.DaoManager;

@RunWith(MockitoJUnitRunner.class)
public class MessagingIT {
	private static List<HasherNode> ahrcs = new LinkedList<>();
	private static List<ResizerNode> arrcs = new LinkedList<>();

	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024;
	private static final int RESIZE_SIZE = 32;
	private static final Path TEST_PATH = Paths.get("foo");

	private static ArtemisEmbeddedServer aes;
	private Database database;
	private ImageRepository imageRepository;
	private static ArtemisHashProducer ahp;
	private static ArtemisSession as;
	private PendingHashImageRepository pendingRepo;

	private Path dbFile;
	private static Path workingdir;
	private static Path testImageAutumnOriginal;
	private Path testImageAutumn;
	private static long testImageAutumnReferenceHash;

	private Duration messageTimeout = new Duration(6, TimeUnit.SECONDS);

	@BeforeClass
	public static void classSetup() throws Exception {
		workingdir = Files.createTempDirectory("MessageIntegration");

		testImageAutumnOriginal = Paths.get(Thread.currentThread().getContextClassLoader().getResource("images/autumn.jpg").toURI());

		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testImageAutumnOriginal))) {
			testImageAutumnReferenceHash = new ImagePHash().getLongHash(bis);
		}

		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testImageAutumnOriginal))) {
			testImageAutumnReferenceHash = new ImagePHash().getLongHash(bis);
		}

		aes = new ArtemisEmbeddedServer(workingdir);
		aes.start();

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD).setBlockOnNonDurableSend(false);
		as = new ArtemisSession(locator);
	}

	@AfterClass
	public static void classTearDown() throws Exception {
		aes.stop();
		TestUtil.deleteAllFiles(workingdir);
	}

	@Before
	public void setup() throws Exception {
		dbFile = Files.createTempFile(workingdir, "database", ".db");
		database = new SQLiteDatabase(dbFile);
		RepositoryFactory repositoryFactory = new OrmliteRepositoryFactory(database);

		testImageAutumn = Files.createTempFile(Paths.get(""), "testImage", ".jpg");
		Files.copy(testImageAutumnOriginal, testImageAutumn, StandardCopyOption.REPLACE_EXISTING);
		imageRepository = repositoryFactory.buildImageRepository();
		pendingRepo = new OrmlitePendingHashImage(DaoManager.createDao(database.getCs(), PendingHashImage.class));
	}

	@After
	public void tearDown() throws Exception {
		database.close();
		Files.delete(testImageAutumn);
	}

	@Test
	public void testHashImage() throws Exception {
		String hashQueue = "hashImageHash";
		String resizeQueue = "hashImageResize";
		String resultQueue = "hashImageResult";
		String queryQueue = "hashImageQuery";
		String eaQueue = "eaqueue";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(resultQueue, resultQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);
		noDupe.createTemporaryQueue(eaQueue, eaQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, pendingRepo, imageRepository);

		new HasherNode(as.getSession(), new ImagePHash(), hashQueue, resultQueue);
		new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage);

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttributeDirectoryCache(new ExtendedAttribute()),
				new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), Collections.emptyList(), resizeQueue, eaQueue);
		ahp = new ArtemisHashProducer(sn);

		RepositoryNode rn = new RepositoryNode(noDupe, queryQueue, resultQueue, pendingRepo, imageRepository);

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
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, pendingRepo, imageRepository);

		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttributeDirectoryCache(new ExtendedAttribute()),
				new HashAttribute(HashNames.DEFAULT_DCT_HASH_2), Collections.emptyList(), resizeQueue, eaQueue);
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);
		ResizerNode arrc = new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage);

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

		pendingRepo.store(new PendingHashImage(testImageAutumn));

		QueryMessage qm = new QueryMessage(noDupe, requestQueue);
		RepositoryNode qr = new RepositoryNode(noDupe, requestQueue, pendingRepo, imageRepository);

		await().atMost(messageTimeout).until(new Callable<List<String>>() {

			@Override
			public List<String> call() throws Exception {
				return qm.pendingImagePaths();
			}
		}, is(containsInAnyOrder(testImageAutumn.toString())));
	}

	@Test(timeout = 5000)
	public void testTrackPath() throws Exception {
		String testqueue = "trackPath";
		as.getSession().createTemporaryQueue(testqueue, testqueue);

		QueryMessage qm = new QueryMessage(as.getSession(), testqueue);
		RepositoryNode qr = new RepositoryNode(as.getSession(), testqueue, pendingRepo, imageRepository);

		assertThat(qm.trackPath(TEST_PATH), is(1));
	}

	@Test
	public void testEAupdated() throws Exception {
		String hashQueue = "eaHash";
		String resizeQueue = "eaResize";
		String resultQueue = "eaResult";
		String queryQueue = "eaeaQuery";
		String eaQueue = "eaeaqueue";

		ClientSession noDupe = as.getSession();
		noDupe.createTemporaryQueue(resizeQueue, resizeQueue);
		noDupe.createTemporaryQueue(hashQueue, hashQueue);
		noDupe.createTemporaryQueue(resultQueue, resultQueue);
		noDupe.createTemporaryQueue(queryQueue, queryQueue);
		noDupe.createTemporaryQueue(eaQueue, eaQueue);

		QueryMessage queryMessage = new QueryMessage(as.getSession(), queryQueue);
		RepositoryNode queryResponder = new RepositoryNode(as.getSession(), queryQueue, resultQueue, pendingRepo, imageRepository,
				new TaskMessageHandler(pendingRepo, imageRepository, as.getSession(), eaQueue));

		new HasherNode(as.getSession(), new ImagePHash(), hashQueue, resultQueue);
		new ResizerNode(as.getSession(), new ImageResizer(RESIZE_SIZE), resizeQueue, hashQueue, queryMessage);

		HashAttribute ha = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		StorageNode sn = new StorageNode(as.getSession(), new ExtendedAttribute(), ha, Collections.emptyList(), resizeQueue, eaQueue);
		ahp = new ArtemisHashProducer(sn);

		RepositoryNode rn = new RepositoryNode(noDupe, queryQueue, resultQueue, pendingRepo, imageRepository);

		assertThat(ha.areAttributesValid(testImageAutumn), is(false));// guard assert

		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).untilCall(to(ha).areAttributesValid(testImageAutumn), is(true));
	}
}
