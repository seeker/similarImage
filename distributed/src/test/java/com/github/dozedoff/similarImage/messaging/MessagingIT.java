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
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.component.DaggerMessagingComponent;
import com.github.dozedoff.similarImage.component.DaggerPersistenceComponent;
import com.github.dozedoff.similarImage.component.MessagingComponent;
import com.github.dozedoff.similarImage.component.PersistenceComponent;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.Thumbnail;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.module.ArtemisModule;
import com.github.dozedoff.similarImage.module.SQLitePersistenceModule;
import com.github.dozedoff.similarImage.util.TestUtil;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

@RunWith(MockitoJUnitRunner.class)
public class MessagingIT {
	private static ArtemisEmbeddedServer aes;

	private static Database database;
	private ImageRepository imageRepository;
	private static ArtemisSession as;
	private PendingHashImageRepository pendingRepo;

	private static Path dbFile;
	private static Path workingdir;
	private static Path testImageAutumnOriginal;
	private Path testImageAutumn;
	private static Path testImageCorruptOriginal;
	private Path testImageCorrupt;
	private static long testImageAutumnReferenceHash;

	private Duration messageTimeout = Duration.ofSeconds(6);
	private ResultMessageSink sink;
	private List<Node> nodes;

	private ClientSession queueJanitor;

	private static PersistenceComponent persistenceComponent;
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
		

		persistenceComponent = DaggerPersistenceComponent.builder()
				.sQLitePersistenceModule(new SQLitePersistenceModule(dbFile))
				.build();
		database = persistenceComponent.getDatabase();

		messageComponent = DaggerMessagingComponent.builder().persistenceComponent(persistenceComponent)
				.artemisModule(new ArtemisModule(workingdir)).build();

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
		nodes = new LinkedList<Node>();
		clearDatabase(database.getCs());
		testImageAutumn = Files.createTempFile(Paths.get(""), "testImage", ".jpg");
		testImageCorrupt = Files.createTempFile(Paths.get(""), "corrupt", ".jpg");

		Files.copy(testImageAutumnOriginal, testImageAutumn, StandardCopyOption.REPLACE_EXISTING);
		Files.copy(testImageCorruptOriginal, testImageCorrupt, StandardCopyOption.REPLACE_EXISTING);

		imageRepository = persistenceComponent.getImageRepository();
		pendingRepo = persistenceComponent.getPendingHashImageRepository();

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
		for (Node node : nodes) {
			node.stop();
		}

		clearDatabase(database.getCs());

		Files.delete(testImageAutumn);
		Files.delete(testImageCorrupt);
		sink.stop();
		as.close();
	}

	@AfterClass
	public static void tearDownClass() throws IOException {
		database.close();
	}

	@Test
	public void testHashImage() throws Exception {
		nodes.add(messageComponent.getRepositoryNode());

		StorageNode sn = messageComponent.getStorageNode();

		nodes.add(sn);
		nodes.add(messageComponent.getResizerNode());

		HasherNode hn = messageComponent.getHasherNode();
		nodes.add(hn);
		nodes.add(messageComponent.getRepositoryNode());
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);

		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).until(() -> imageRepository.getByHash(testImageAutumnReferenceHash),
				containsInAnyOrder(new ImageRecord(testImageAutumn.toString(), testImageAutumnReferenceHash)));
	}

	@Test
	public void testDoNotQueueDuplicates() throws Exception {
		ClientSession noDupe = as.getSession();

		nodes.add(messageComponent.getRepositoryNode());

		StorageNode sn = messageComponent.getStorageNode();
		nodes.add(sn);
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);
		nodes.add(messageComponent.getResizerNode());

		ahp.handle(testImageAutumn);
		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).until(new Callable<Long>() {

			@Override
			public Long call() throws Exception {
				return noDupe.queueQuery(new SimpleString(QueueAddress.HASH_REQUEST.toString())).getMessageCount();
			}
		}, is(1L));

		noDupe.close();
	}

	@Test
	public void testPendingImagesQuery() throws Exception {
		ClientSession noDupe = as.getSession();

		pendingRepo.store(new PendingHashImage(testImageAutumn, UUID.randomUUID()));

		QueryMessage qm = new QueryMessage(noDupe, QueueAddress.REPOSITORY_QUERY.toString());
		nodes.add(messageComponent.getRepositoryNode());

		await().atMost(messageTimeout).until(new Callable<List<String>>() {

			@Override
			public List<String> call() throws Exception {
				return qm.pendingImagePaths();
			}
		}, is(containsInAnyOrder(testImageAutumn.toString())));
	}

	@Test
	public void testEAupdated() throws Exception {
		nodes.add(messageComponent.getRepositoryNode());
		nodes.add(messageComponent.getHasherNode());
		nodes.add(messageComponent.getResizerNode());

		HashAttribute ha = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);

		StorageNode sn = messageComponent.getStorageNode();
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);

		assertThat(ha.areAttributesValid(testImageAutumn), is(false));// guard assert

		ahp.handle(testImageAutumn);

		await().atMost(messageTimeout).until(() -> ha.areAttributesValid(testImageAutumn), is(true));
	}

	@Test
	public void testMarkCorrupt() throws Exception {
		nodes.add(messageComponent.getRepositoryNode());

		nodes.add(messageComponent.getHasherNode());
		nodes.add(messageComponent.getResizerNode());

		HashAttribute ha = new HashAttribute(HashNames.DEFAULT_DCT_HASH_2);
		StorageNode sn = messageComponent.getStorageNode();
		ArtemisHashProducer ahp = new ArtemisHashProducer(sn);

		assertThat(ha.isCorrupted(testImageCorrupt), is(false));// guard assert

		ahp.handle(testImageCorrupt);

		await().atMost(messageTimeout).until(() -> ha.isCorrupted(testImageCorrupt), is(true));
	}
}
