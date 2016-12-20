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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.j256.ormlite.misc.TransactionManager;

@RunWith(MockitoJUnitRunner.class)
public class QueueToDatabaseTransactionTest {
	private static final int TEST_MESSAGE_SIZE = 5;
	private static final String EXCEPTION_MESSAGE = "Testing";

	private static final long UUID_MOST = 5;
	private static final long UUID_LEAST = 7;
	private static final long HASH = 42;
	private static final String PATH = Paths.get("foo/bar").toString();

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private PendingHashImageRepository pendingRepository;

	@Mock
	private ClientSession session;

	@Mock
	private TransactionManager transactionManager;

	@Mock
	private ClientMessage message;

	@Mock
	private ClientMessage sendMessage;

	@Mock
	private ClientProducer producer;

	@Mock
	private ActiveMQBuffer buffer;

	private List<ClientMessage> messages;

	@Captor
	private ArgumentCaptor<Callable<Void>> transactionCall;

	private MetricRegistry metrics;

	private QueueToDatabaseTransaction cut;

	@Before
	public void setUp() throws Exception {
		when(message.getBodyBuffer()).thenReturn(buffer);
		when(buffer.readLong()).thenReturn(UUID_MOST, UUID_LEAST, HASH);
		when(pendingRepository.getByUUID(UUID_MOST, UUID_LEAST))
				.thenReturn(new PendingHashImage(PATH, UUID_MOST, UUID_LEAST));
		
		when(session.createMessage(anyBoolean())).thenReturn(sendMessage);
		when(session.createProducer(anyString())).thenReturn(producer);
		when(sendMessage.getBodyBuffer()).thenReturn(buffer);

		setUpMessages();

		metrics = new MetricRegistry();

		cut = new QueueToDatabaseTransaction(session, transactionManager, pendingRepository,
				imageRepository, metrics);
	}

	private void setUpMessages() {
		messages = new LinkedList<ClientMessage>();

		for (int i = 0; i < TEST_MESSAGE_SIZE; i++) {
			messages.add(message);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonTransactedSessionAck() throws Exception {
		when(session.isAutoCommitAcks()).thenReturn(true);

		new QueueToDatabaseTransaction(session, transactionManager, pendingRepository, imageRepository, metrics);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonTransactedSessionSend() throws Exception {
		when(session.isAutoCommitSends()).thenReturn(true);

		new QueueToDatabaseTransaction(session, transactionManager, pendingRepository, imageRepository, metrics);
	}

	@Test
	public void testTransactedSession() throws Exception {
		new QueueToDatabaseTransaction(session, transactionManager, pendingRepository, imageRepository, metrics);
	}

	@Test
	public void testOnDrainInTransaction() throws Exception {
		cut.onDrain(messages);

		verify(transactionManager).callInTransaction(any());
	}

	@Test
	public void testMessageRollbackOnDatabaseFailure() throws Exception {
		when(transactionManager.callInTransaction(any()))
				.thenThrow(new SQLException(EXCEPTION_MESSAGE));

		cut.onDrain(messages);

		verify(session).rollback();
	}

	@Test(expected = RuntimeException.class)
	public void testMessageRollbackFailure() throws Exception {
		when(transactionManager.callInTransaction(any())).thenThrow(new SQLException(EXCEPTION_MESSAGE));

		Mockito.doThrow(new ActiveMQException()).when(session).rollback();

		cut.onDrain(messages);
	}

	@Test
	public void testOnCallMessageAcknowledge() throws Exception {
		cut.onCall(messages);

		verify(message, times(TEST_MESSAGE_SIZE)).acknowledge();
	}

	@Test
	public void testOnCallPendingQuery() throws Exception {
		cut.onCall(messages);
		
		verify(pendingRepository).getByUUID(UUID_MOST, UUID_LEAST);
	}

	@Test
	public void testOnCallPendingMessageMissing() throws Exception {
		when(pendingRepository.getByUUID(UUID_MOST, UUID_LEAST)).thenReturn(null);

		cut.onCall(messages);

		assertThat(
				metrics.getCounters().get(QueueToDatabaseTransaction.METRIC_NAME_PENDING_MESSAGES_MISSING).getCount(),
				is((long) TEST_MESSAGE_SIZE));
	}

	@Test
	public void testOnCallPendingMessagesDecrement() throws Exception {
		cut.onCall(messages);

		assertThat(metrics.getCounters().get(QueueToDatabaseTransaction.METRIC_NAME_PENDING_MESSAGES).getCount(),
				is(-1L));
	}

	@Test
	public void testOnCallProcessedMessagesIncrement() throws Exception {
		cut.onCall(messages);

		assertThat(metrics.getMeters().get(QueueToDatabaseTransaction.METRIC_NAME_PROCESSED_IMAGES).getCount(),
				is(1L));
	}

	@Test
	public void testOnCallResultStored() throws Exception {
		cut.onCall(messages);

		verify(imageRepository).store(eq(new ImageRecord(PATH, HASH)));
	}

	@Test
	public void testOnCallPendingRemoved() throws Exception {
		cut.onCall(messages);

		verify(pendingRepository).remove(new PendingHashImage(PATH, UUID_MOST, UUID_LEAST));
	}

	@Test
	public void testOnCallEAupdateSent() throws Exception {
		cut.onCall(messages);

		verify(producer).send(sendMessage);
	}
}
