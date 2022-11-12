package com.github.dozedoff.similarImage.messaging;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;

public class StorageNodeTestIT extends MessagingBaseTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final long MOCKITO_TIMEOUT = 2000;
	
	private static final Path PATH = Paths.get("foo");
	private static final long HASH = 42;

	@Mock
	private ExtendedAttributeQuery eaQuery;

	@Mock
	private HashAttribute hashAttribute;

	private StorageNode cut;
	private MessageFactory messageFactory;
	private Path testFile;
	private Path cachedFile;
	
	private ClientMessage message;
	private ClientConsumer consumer;
	private ClientProducer producer;
	private List<ClientMessage> messages;
	
	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
		testFile = Files.createTempFile("StorageNodeTest", null);
		cachedFile = Files.createTempFile("StorageNodeTest", null);

		messages = new LinkedList<>();
		
		cut = new StorageNode(session, eaQuery, hashAttribute, Arrays.asList(cachedFile));
		
		producer = session.createProducer(QueueAddress.EA_UPDATE.toString());
		consumer = session.createConsumer(QueueAddress.RESIZE_REQUEST.toString());
		consumer.setMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(ClientMessage message) {
				messages.add(message);
			}
		});
	}

	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(testFile);
		Files.deleteIfExists(cachedFile);
	}

	@Test
	public void testOnMessageEaUpdate() throws Exception {
		message = messageFactory.eaUpdate(PATH, HASH);
		producer.send(message);

		
		verify(hashAttribute, timeout(MOCKITO_TIMEOUT).times(1)).writeHash(PATH, HASH);
	}

	@Test
	public void testOnMessageCorrupt() throws Exception {
		message = messageFactory.corruptMessage(PATH);
		producer.send(message);

		verify(hashAttribute, timeout(MOCKITO_TIMEOUT).times(1)).markCorrupted(PATH);
	}

	@Test
	public void testOnMessageWrong() throws Exception {
		message = messageFactory.pendingImageQuery();
		producer.send(message);

		verify(hashAttribute, never()).writeHash(PATH, HASH);
		verify(hashAttribute, never()).markCorrupted(PATH);
	}

	@Test
	public void testProcessFile() throws Exception {
		cut.processFile(testFile);

		await().until(messages::size, is(1));
		
		ClientMessage message = messages.get(0);
		assertThat(message.getStringProperty(MessageProperty.path.toString()), is(testFile.toString()));
	}

	@Test
	public void testProcessFileSkipDuplicate() throws Exception {
		cut.processFile(testFile);
		cut.processFile(testFile);

		await().pollDelay(1, TimeUnit.SECONDS).until(messages::size, is(1));
		
		ClientMessage message = messages.get(0);
		assertThat(message.getStringProperty(MessageProperty.path.toString()), is(testFile.toString()));
		assertThat(messages.size(), is(1));
	}

	@Test
	public void testProcessFileSkipDuplicateCached() throws Exception {
		cut.processFile(cachedFile);

		await().pollDelay(1, TimeUnit.SECONDS).until(messages::size, is(0));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("StorageNode"));
	}
}
