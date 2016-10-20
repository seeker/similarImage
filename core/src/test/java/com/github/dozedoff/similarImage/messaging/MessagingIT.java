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
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.db.Database;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.SQLiteDatabase;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.ormlite.OrmliteRepositoryFactory;
import com.github.dozedoff.similarImage.db.repository.ormlite.RepositoryFactory;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.handler.HashNames;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeDirectoryCache;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.util.TestUtil;
import com.j256.ormlite.table.TableUtils;

public class MessagingIT {
	private static List<ArtemisHashRequestConsumer> ahrcs = new LinkedList<>();
	private static List<ArtemisResizeRequestConsumer> arrcs = new LinkedList<>();

	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024;
	private static final int RESIZE_SIZE = 32;

	private static ArtemisResultConsumer arc;
	private static ArtemisEmbeddedServer aes;
	private static Database database;
	private static ImageRepository imageRepository;
	private static ArtemisHashProducer ahp;

	private static Path workingdir;
	private static Path testImageAutumn;
	private static long testImageAutumnReferenceHash;

	@BeforeClass
	public static void classSetup() throws Exception {
		workingdir = Files.createTempDirectory("MessageIntegration");

		testImageAutumn = Paths
				.get(Thread.currentThread().getContextClassLoader().getResource("images/autumn.jpg").toURI());

		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testImageAutumn))) {
			testImageAutumnReferenceHash = new ImagePHash().getLongHash(bis);
			System.out.println(testImageAutumnReferenceHash);
		}
		
		try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testImageAutumn))){
			testImageAutumnReferenceHash = new ImagePHash().getLongHash(bis);
		}

		aes = new ArtemisEmbeddedServer(workingdir);
		aes.start();

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD).setBlockOnNonDurableSend(false)
				.setPreAcknowledge(true);
		ArtemisSession as = new ArtemisSession(locator);
		ArtemisQueue aq = new ArtemisQueue(as.getSession());
		aq.createAll();

		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			ahrcs.add(new ArtemisHashRequestConsumer(as.getSession(), new ImagePHash(), QueueAddress.HASH_REQUEST.toString(),
					QueueAddress.RESULT.toString()));
			arrcs.add(new ArtemisResizeRequestConsumer(as.getSession(), new ImageResizer(RESIZE_SIZE),
					QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.HASH_REQUEST.toString()));
		}

		database = new SQLiteDatabase(Files.createTempFile(workingdir, "database", ".db"));
		RepositoryFactory repositoryFactory = new OrmliteRepositoryFactory(database);

		imageRepository = repositoryFactory.buildImageRepository();

		ExtendedAttributeQuery eaQuery = new ExtendedAttributeDirectoryCache(new ExtendedAttribute(), 1, TimeUnit.MINUTES);
		ahp = new ArtemisHashProducer(as.getSession(), QueueAddress.HASH_REQUEST.toString());

		arc = new ArtemisResultConsumer(as.getSession(), imageRepository, eaQuery, new HashAttribute(HashNames.DEFAULT_DCT_HASH_2));
	}

	@AfterClass
	public static void classTearDown() throws Exception {
		aes.stop();
		TestUtil.deleteAllFiles(workingdir);
	}

	@Before
	public void setup() throws Exception {
		TableUtils.createTableIfNotExists(database.getCs(), ImageRecord.class);
		TableUtils.clearTable(database.getCs(), ImageRecord.class);
	}

	@Test
	public void testHashImage() throws Exception {
		ahp.handle(testImageAutumn);

		await().atMost(5, TimeUnit.SECONDS).untilCall(to(imageRepository).getByHash(testImageAutumnReferenceHash),
				containsInAnyOrder(new ImageRecord(testImageAutumn.toString(), testImageAutumnReferenceHash)));
	}
}
