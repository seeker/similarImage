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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.messaging.QueueToDatabaseTransaction.TransactionCall;
import com.j256.ormlite.misc.TransactionManager;

@RunWith(MockitoJUnitRunner.class)
public class QueueToDatabaseTransactionTest {
	private static final String QUEUE_NAME = "test";

	private static final int TEST_MESSAGE_SIZE = 5;

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

	private List<ClientMessage> messages;

	@Captor
	private ArgumentCaptor<Callable<Void>> transactionCall;

	private QueueToDatabaseTransaction cut;

	@Before
	public void setUp() throws Exception {
		setUpMessages();

		cut = new QueueToDatabaseTransaction(session, transactionManager, QUEUE_NAME, pendingRepository,
				imageRepository);
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

		new QueueToDatabaseTransaction(session, transactionManager, QUEUE_NAME, pendingRepository, imageRepository);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonTransactedSessionSend() throws Exception {
		when(session.isAutoCommitSends()).thenReturn(true);

		new QueueToDatabaseTransaction(session, transactionManager, QUEUE_NAME, pendingRepository, imageRepository);
	}

	@Test
	public void testTransactedSession() throws Exception {
		new QueueToDatabaseTransaction(session, transactionManager, QUEUE_NAME, pendingRepository, imageRepository);
	}

	@Test
	public void testOnDrainInTransaction() throws Exception {
		cut.onDrain(messages);

		verify(transactionManager).callInTransaction(any(TransactionCall.class));
	}
}
