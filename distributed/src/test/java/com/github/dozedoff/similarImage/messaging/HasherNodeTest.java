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

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

	@Before
	public void setUp() throws Exception {
		when(hasher.getLongHash(any(BufferedImage.class))).thenReturn(TEST_HASH);
		messageFactory = new MessageFactory(session);
		message = messageFactory.hashRequestMessage(TEST_DATA, TEST_UUID);
		metrics = new MetricRegistry();

		cut = new HasherNode(session, hasher, TEST_ADDRESS_REQUEST, TEST_ADDRESS_RESULT, metrics);
	}

	@Test
	public void testMessageSent() throws Exception {
		cut.onMessage(message);

		verify(producer).send(sessionMessage);
	}

	@Test
	public void testMessageUuid() throws Exception {
		cut.onMessage(message);

		assertThat(sessionMessage.getBodyBuffer().readLong(), is(TEST_UUID.getMostSignificantBits()));
		assertThat(sessionMessage.getBodyBuffer().readLong(), is(TEST_UUID.getLeastSignificantBits()));
	}

	@Test
	public void testMessageHash() throws Exception {
		cut.onMessage(message);

		sessionMessage.getBodyBuffer().readLong();
		sessionMessage.getBodyBuffer().readLong();

		assertThat(sessionMessage.getBodyBuffer().readLong(), is(TEST_HASH));
	}

	@Test
	public void testMessageCorruptImageSent() throws Exception {
		message = new MockMessageBuilder().configureCorruptImageMessage().build();
		when(hasher.getLongHash(any(InputStream.class))).thenThrow(new IIOException(""));

		cut.onMessage(message);

		verify(producer).send(sessionMessage);
	}
	
	@Test
	public void testMetricsMultipleInstance() throws Exception {
		List<HasherNode> nodes = new LinkedList<HasherNode>();

		for (long i = 0; i < WORKER_NUMBER; i++) {
			nodes.add(new HasherNode(session, hasher, TEST_ADDRESS_REQUEST, TEST_ADDRESS_RESULT, metrics));
			cut.onMessage(message);
		}

		assertThat(metrics.getMeters().get(HasherNode.METRIC_NAME_HASH_MESSAGES)
				.getCount(), is(WORKER_NUMBER));
	}

	@Test
	public void testBufferResize() throws Exception {
		message = messageFactory.hashRequestMessage(new byte[LARGE_DATA_SIZE], TEST_UUID);

		cut.onMessage(message);

		assertThat(metrics.getMeters().get(HasherNode.METRIC_NAME_BUFFER_RESIZE).getCount(), is(1L));
	}

	@Test
	public void testHashDuration() throws Exception {
		cut.onMessage(message);

		assertThat(metrics.getTimers().get(HasherNode.METRIC_NAME_HASH_DURATION).getSnapshot().getMean(), is(not(0.0)));
	}

	@Test
	public void testHashDurationOnFailure() throws Exception {
		when(hasher.getLongHash(any(BufferedImage.class))).thenThrow(new IOException("Testing"));

		cut.onMessage(message);

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

		verify(consumer).close();
		verify(producer).close();
	}
}
