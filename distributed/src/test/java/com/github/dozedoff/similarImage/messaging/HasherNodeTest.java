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
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.commonj.hash.ImagePHash;

@RunWith(MockitoJUnitRunner.class)
public class HasherNodeTest extends MessagingBaseTest {
	private static final String TEST_ADDRESS_REQUEST = "test_request";
	private static final String TEST_ADDRESS_RESULT = "test_result";
	private static final long TEST_HASH = 42L;
	private static final byte[] TEST_DATA = { 12, 13, 14, 15, 16 };
	private static final UUID TEST_UUID = new UUID(12, 42);
	private static final long WORKER_NUMBER = 10L;
	private static final int LARGE_DATA_SIZE = 5000;

	@Mock
	private ImagePHash hasher;

	private HasherNode cut;
	private MessageFactory messageFactory;
	private MetricRegistry metrics;
	private ClientMessage hashRequestMessage;
	private ClientConsumer consumer;
	private ClientProducer producer;

	private List<ClientMessage> messages;
	
	@Before
	public void setUp() throws Exception {
		when(hasher.getLongHash(nullable(BufferedImage.class))).thenReturn(TEST_HASH);
		messageFactory = new MessageFactory(session);
		hashRequestMessage = messageFactory.hashRequestMessage(TEST_DATA, TEST_UUID);
		metrics = new MetricRegistry();

		cut = new HasherNode(session, hasher, TEST_ADDRESS_REQUEST, TEST_ADDRESS_RESULT, metrics);
		consumer = session.createConsumer(TEST_ADDRESS_RESULT);
		producer = session.createProducer(TEST_ADDRESS_REQUEST);
		
		messages = new LinkedList<>();
		
		consumer.setMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(ClientMessage message) {
				messages.add(message);
			}
		});
	}

	private ClientMessage waitForHashResult() throws ActiveMQException {
		producer.send(hashRequestMessage);
		
		await().until(messages::size, is(1));
		
		return messages.get(0);
	}
	
	@Test
	public void testMessageSentAndResponseReceived() throws Exception {
		producer.send(hashRequestMessage);
		
		await().until(messages::size, is(1));
	}

	@Test
	public void testMessageUuid() throws Exception {
		ClientMessage result = waitForHashResult();
		
		long mostSignificant = result.getBodyBuffer().readLong();
		long leastSignificant = result.getBodyBuffer().readLong();

		assertThat(mostSignificant, is(TEST_UUID.getMostSignificantBits()));
		assertThat(leastSignificant, is(TEST_UUID.getLeastSignificantBits()));
	}

	@Test
	public void testMessageHash() throws Exception {
		ClientMessage result = waitForHashResult();

		result.getBodyBuffer().readLong();
		result.getBodyBuffer().readLong();

		assertThat(result.getBodyBuffer().readLong(), is(TEST_HASH));
	}

	@Test
	public void testMessageCorruptImageSent() throws Exception {
		Mockito.reset(hasher);
		when(hasher.getLongHash(nullable(BufferedImage.class))).thenThrow(new IIOException(""));
		
		producer.send(hashRequestMessage);
		
		await().pollDelay(1, TimeUnit.SECONDS).until(messages::size, is(not(1)));
	}
	
	@Test
	public void testMetricsMultipleInstance() throws Exception {
		List<HasherNode> nodes = new LinkedList<HasherNode>();

		for (long i = 0; i < WORKER_NUMBER; i++) {
			nodes.add(new HasherNode(session, hasher, TEST_ADDRESS_REQUEST, TEST_ADDRESS_RESULT, metrics));
			producer.send(hashRequestMessage);
		}
		
		await().until(messages::size, is((int)WORKER_NUMBER));

		assertThat(metrics.getMeters().get(HasherNode.METRIC_NAME_HASH_MESSAGES)
				.getCount(), is(WORKER_NUMBER));
	}

	@Test
	public void testBufferResize() throws Exception {
		hashRequestMessage = messageFactory.hashRequestMessage(new byte[LARGE_DATA_SIZE], TEST_UUID);

		cut.onMessage(hashRequestMessage);

		await().until(messages::size, is(1));

		assertThat(metrics.getMeters().get(HasherNode.METRIC_NAME_BUFFER_RESIZE).getCount(), is(1L));
	}

	@Test
	public void testHashDuration() throws Exception {
		cut.onMessage(hashRequestMessage);

		await().until(messages::size, is(1));

		assertThat(metrics.getTimers().get(HasherNode.METRIC_NAME_HASH_DURATION).getSnapshot().getMean(), is(not(0.0)));
	}

	@Test
	public void testHashDurationOnFailure() throws Exception {
		Mockito.reset(hasher);
		when(hasher.getLongHash(nullable(BufferedImage.class))).thenThrow(new IOException("Testing"));

		cut.onMessage(hashRequestMessage);

		assertThat(metrics.getTimers().get(HasherNode.METRIC_NAME_HASH_DURATION).getSnapshot().getMean(), is(0.0));
	}

	@Test
	public void testToStringStart() throws Exception {
		assertThat(cut.toString(), startsWith("HasherNode {"));
	}

	@Test
	public void testToStringEnd() throws Exception {
		assertThat(cut.toString(), endsWith("}"));
	}

	@Test
	public void testStop() throws Exception {
		cut.stop();

		assertThat(session.isClosed(), is(true));
	}
}
