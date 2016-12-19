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

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.j256.ormlite.misc.TransactionManager;

public class QueueToDatabaseTransaction implements CollectedMessageConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueueToDatabaseTransaction.class);

	private final ClientConsumer consumer;
	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;
	private final TransactionManager transactionManager;

	public QueueToDatabaseTransaction(ClientSession session, TransactionManager transactionManager,
			String resultQueueName,
			PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository) throws ActiveMQException {

		if (!isTransactedSession(session)) {
			throw new IllegalArgumentException("Session must be transactional");
		}

		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
		this.consumer = session.createConsumer(resultQueueName);
		this.transactionManager = transactionManager;
	}

	private boolean isTransactedSession(ClientSession session) {
		return !(session.isAutoCommitAcks() || session.isAutoCommitSends());
	}

	@Override
	public void onDrain(List<ClientMessage> messages) {
		try {
			transactionManager.callInTransaction(new TransactionCall(messages));
		} catch (SQLException e) {
			LOGGER.warn("Failed to store {} messages: {}", messages.size(), e.toString());
		}
	}

	protected class TransactionCall implements Callable<Void> {
		private final List<ClientMessage> messages;

		public TransactionCall(List<ClientMessage> messages) {
			this.messages = messages;
		}

		@Override
		public Void call() throws Exception {
			for (ClientMessage message : messages) {
				// TODO Auto-generated method stub
			}

			return null;
		}

	}
}
