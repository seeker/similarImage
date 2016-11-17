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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;

@RunWith(MockitoJUnitRunner.class)
public class TaskMessageHandlerTest extends MessagingBaseTest {
	private static final long TEST_HASH = 42L;
	private static final String TEST_PATH = "foo";
	private static final UUID UUID = new UUID(99, 100);
	private static final long MOST = UUID.getMostSignificantBits();
	private static final long LEAST = UUID.getLeastSignificantBits();

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private PendingHashImageRepository pendingRepository;

	private TaskMessageHandler cut;

	private MessageFactory messageFactory;
	private MetricRegistry metrics;

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
		metrics = new MetricRegistry();
		cut = new TaskMessageHandler(pendingRepository, imageRepository, session, metrics);
	}

	@Test
	public void testStoreHash() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(new PendingHashImage(TEST_PATH, MOST, LEAST));

		cut.onMessage(message);

		verify(imageRepository).store(new ImageRecord(TEST_PATH, TEST_HASH));
	}

	@Test
	public void testSendEaUpdate() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(new PendingHashImage(TEST_PATH, MOST, LEAST));

		cut.onMessage(message);

		verify(producer).send(messageFactory.eaUpdate(Paths.get(TEST_PATH), TEST_HASH));
	}

	@Test
	public void testMetricPendingMessagesResult() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(new PendingHashImage(TEST_PATH, MOST, LEAST));

		cut.onMessage(message);

		assertThat(metrics.getCounters().get(TaskMessageHandler.METRIC_NAME_PENDING_MESSAGES).getCount(), is(-1L));
	}

	@Test
	public void testMetricPendingMessagesTrack() throws Exception {
		message = messageFactory.trackPath(Paths.get(TEST_PATH), UUID);

		cut.onMessage(message);

		assertThat(metrics.getCounters().get(TaskMessageHandler.METRIC_NAME_PENDING_MESSAGES).getCount(), is(1L));
	}

	@Test
	public void testMetricPendingMessagesMissed() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(null);

		cut.onMessage(message);

		assertThat(metrics.getCounters().get(TaskMessageHandler.METRIC_NAME_PENDING_MESSAGES_MISSING).getCount(),
				is(1L));
	}
}
