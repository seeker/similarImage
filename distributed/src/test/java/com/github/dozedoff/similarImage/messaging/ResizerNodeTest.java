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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;
import com.google.common.io.BaseEncoding;

@RunWith(MockitoJUnitRunner.class)
public class ResizerNodeTest extends MessagingBaseTest {
	private static final Duration MESSAGE_TIMEOUT = Duration.ofSeconds(2);
	
	private static final String PATH = "bar";
	private static final String PATH_NEW = "foo";
	private static final int BUFFER_TEST_DATA_SIZE = 100;

	@Mock
	private ImageResizer resizer;

	@Mock
	private InputStream is;

	@Mock
	private PendingHashImageRepository pendingRepo;

	@Mock
	private QueryMessage queryMessage;

	private ResizerNode cut;

	private MessageFactory messageBuilder;
	private MetricRegistry metrics;
	
	private ClientMessage message;
	private ClientProducer producer;
	private ClientConsumer hashRequestConsumer;
	private ClientConsumer eaUpdateConsumer;
	private ClientConsumer resultConsumer;
	
	private List<ClientMessage> hashRequests;
	private List<ClientMessage> eaUpdates;
	private List<ClientMessage> results;

	@Before
	public void setUp() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenReturn(new byte[0]);
		when(queryMessage.pendingImagePaths()).thenReturn(Arrays.asList(PATH));
		when(is.read()).thenReturn(-1);

		metrics = new MetricRegistry();
		
		producer = session.createProducer(QueueAddress.RESIZE_REQUEST.toString());
		
		hashRequests = new LinkedList<>();
		hashRequestConsumer = session.createConsumer(QueueAddress.HASH_REQUEST.toString());
		hashRequestConsumer.setMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(ClientMessage message) {
				hashRequests.add(message);
			}
		});
		
		
		
		eaUpdates = new LinkedList<>();
		eaUpdateConsumer = session.createConsumer(QueueAddress.EA_UPDATE.toString());
		eaUpdateConsumer.setMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(ClientMessage message) {
				eaUpdates.add(message);
			}
		});
		
		results = new LinkedList<>();
		resultConsumer = session.createConsumer(QueueAddress.RESULT.toString());
		resultConsumer.setMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(ClientMessage message) {
				results.add(message);
			}
		});
		
		cut = new ResizerNode(session, resizer, QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.HASH_REQUEST.toString(), queryMessage, metrics);
		messageBuilder = new MessageFactory(session);
		message = messageBuilder.resizeRequest(Paths.get(PATH_NEW), is);
	}
	
	@After
	public void tearDown() throws ActiveMQException {
		resultConsumer.close();
		eaUpdateConsumer.close();
	}

	@Test
	public void testValidImageSent() throws Exception {
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(hashRequests::size, is(1));
	}

	@Test
	public void testValidImageHasId() throws Exception {
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(hashRequests::size, is(1));
		
		ClientMessage response = hashRequests.get(0);

		assertThat(response.getBodyBuffer().readLong(), is(not(0L)));
		assertThat(response.getBodyBuffer().readLong(), is(not(0L)));
	}

	@Test
	public void testValidImageNotCorrupt() throws Exception {
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(hashRequests::size, is(1));
		
		ClientMessage response = hashRequests.get(0);

		assertThat(response.containsProperty(MessageProperty.task.toString()), is(false));
	}

	@Test
	public void testValidImageTrackSent() throws Exception {
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(results::size, is(1));
		
		ClientMessage response = results.get(0);

		assertThat(response.getStringProperty(MessageProperty.task.toString()), is(TaskType.track.toString()));
	}

	@Test
	public void testInvalidImageDataResponseMessageSent() throws Exception {
		// Signature / magic number for JPG taken from https://en.wikipedia.org/wiki/List_of_file_signatures
		// JPG signature: FF D8 FF DB
		
		message = messageBuilder.resizeRequest(Paths.get(PATH_NEW), new ByteArrayInputStream(BaseEncoding.base16().decode("FFD8FFDB")));
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(1));
	}

	@Test
	public void testCorruptImageDataTaskProperty() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenThrow(new IIOException(""));

		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(1));
		
		ClientMessage response = eaUpdates.get(0);

		assertThat(response.getStringProperty(MessageProperty.task.toString()),
				is(TaskType.corr.toString()));
	}

	@Test
	public void testCorruptImageDataPathProperty() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenThrow(new IIOException(""));
		
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(1));
		
		ClientMessage response = eaUpdates.get(0);

		assertThat(response.getStringProperty(MessageProperty.path.toString()), is(PATH_NEW));
	}

	@Test
	public void testImageReadError() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenThrow(new IOException("testing"));

		producer.send(message);

		await().pollDelay(1, TimeUnit.SECONDS).atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(0));
		await().pollDelay(1, TimeUnit.SECONDS).atMost(MESSAGE_TIMEOUT).until(hashRequests::size, is(0));
	}

	@Test
	public void testGIFerrorUnknownBlock() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenThrow(new IOException("Unknown block"));
		
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(1));
		ClientMessage response = eaUpdates.get(0);
		assertThat(response.getStringProperty(MessageProperty.task.toString()),
				is(TaskType.corr.toString()));
	}

	@Test
	public void testGIFerrorInvalidHeader() throws Exception {
		when(resizer.resize(nullable(BufferedImage.class))).thenThrow(new IOException("Invalid GIF header"));

		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(1));
		ClientMessage response = eaUpdates.get(0);
		assertThat(response.getStringProperty(MessageProperty.task.toString()),
				is(TaskType.corr.toString()));
	}

	@Test
	public void testDuplicatePreLoaded() throws Exception {
		message = messageBuilder.resizeRequest(Paths.get(PATH), is);

		producer.send(message);

		await().alias("EA update message").pollDelay(1, TimeUnit.SECONDS).atMost(MESSAGE_TIMEOUT).until(eaUpdates::size, is(0));
		await().alias("Hash message").pollDelay(1, TimeUnit.SECONDS).atMost(MESSAGE_TIMEOUT).until(hashRequests::size, is(0));
	}

	@Test
	public void testDuplicatePreLoadedMetricCacheHit() throws Exception {
		message = messageBuilder.resizeRequest(Paths.get(PATH), is);

		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(() -> metrics.getMeters().get(ResizerNode.METRIC_NAME_PENDING_CACHE_HIT).getCount(), is(1L));
	}

	@Test
	public void testDuplicatePreLoadedMetricCacheMiss() throws Exception {
		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(() -> metrics.getMeters().get(ResizerNode.METRIC_NAME_PENDING_CACHE_MISS).getCount(), is(1L));
	}

	@Test
	public void testBufferResize() throws Exception {
		for (byte i = 0; i < BUFFER_TEST_DATA_SIZE; i++) {
			message.getBodyBuffer().writeByte(i);
		}
		cut.allocateNewBuffer(1);

		producer.send(message);

		await().atMost(MESSAGE_TIMEOUT).until(() -> metrics.getCounters().get(ResizerNode.METRIC_NAME_BUFFER_RESIZE).getCount(), is(1L));
	}

	@Test
	public void testMetricsResizeRequest() throws Exception {
		producer.send(message);
		
		await().until(() -> metrics.getMeters().get(ResizerNode.METRIC_NAME_RESIZE_MESSAGES).getCount(), is(1L));
	}

	@Test
	public void testImageSizeHistogramCount() throws Exception {
		message = new MessageFactory(session).resizeRequest(Paths.get(PATH_NEW), is);

		for (byte i = 0; i < BUFFER_TEST_DATA_SIZE; i++) {
			message.getBodyBuffer().writeByte(i);
		}

		cut.onMessage(message);

		assertThat(metrics.getHistograms().get(ResizerNode.METRIC_NAME_IMAGE_SIZE).getCount(), is(1L));
	}

	@Test
	public void testImageSizeHistogramSize() throws Exception {
		message = new MessageFactory(session).resizeRequest(Paths.get(PATH_NEW), is);

		for (byte i = 0; i < BUFFER_TEST_DATA_SIZE; i++) {
			message.getBodyBuffer().writeByte(i);
		}

		cut.onMessage(message);

		assertThat(metrics.getHistograms().get(ResizerNode.METRIC_NAME_IMAGE_SIZE).getSnapshot().getMean(),
				is((double) BUFFER_TEST_DATA_SIZE));
	}

	@Test
	public void testResizeTime() throws Exception {
		producer.send(message);

		await().pollDelay(1, TimeUnit.SECONDS).until(() -> metrics.getTimers().get(ResizerNode.METRIC_NAME_RESIZE_DURATION).getSnapshot().getMean(), is(not(0.0)));
	}

	@Test
	public void testToStringStart() throws Exception {
		assertThat(cut.toString(), startsWith("ResizerNode {"));
	}

	@Test
	public void testToStringEnd() throws Exception {
		assertThat(cut.toString(), endsWith("}"));
	}
}
