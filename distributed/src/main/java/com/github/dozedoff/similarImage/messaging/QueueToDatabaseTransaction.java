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

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.j256.ormlite.misc.TransactionManager;

public class QueueToDatabaseTransaction implements CollectedMessageConsumer {
	private static final String MESSAGES = "messages";
	private static final String PENDING = "pending";

	private static final Logger LOGGER = LoggerFactory.getLogger(QueueToDatabaseTransaction.class);

	public static final String METRIC_NAME_PENDING_MESSAGES_MISSING = MetricRegistry.name(RepositoryNode.class,
			PENDING, MESSAGES, "missing");
	public static final String METRIC_NAME_PENDING_MESSAGES = MetricRegistry.name(TaskMessageHandler.class, PENDING,
			MESSAGES);
	public static final String METRIC_NAME_PROCESSED_IMAGES = MetricRegistry.name(TaskMessageHandler.class, "processed",
			"images");

	private final ClientSession session;
	private final ClientProducer producer;
	private final MessageFactory messageFactory;
	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;
	private final TransactionManager transactionManager;

	private final Counter pendingMessages;
	private final Counter pendingMessagesMissing;
	private final Meter processedImages;

	/**
	 * Create an instance to manage queue to database transactions. Uses the default queues.
	 * 
	 * @param transactedSession
	 *            session with transactions enabled
	 * @param transactionManager
	 *            for transacted database operations
	 * @param eaQueueName
	 *            the name of the queue for EA updates
	 * @param pendingRepository
	 *            repository for storing pending image records
	 * @param imageRepository
	 *            repository for storing results
	 * @param metrics
	 *            for tracking metrics
	 */
	public QueueToDatabaseTransaction(ClientSession transactedSession, TransactionManager transactionManager,
			String eaQueueName, PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository, MetricRegistry metrics) {

		if (!isTransactedSession(transactedSession)) {
			throw new IllegalArgumentException("Session must be transactional");
		}

		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
		this.session = transactedSession;
		try {
			this.producer = transactedSession.createProducer(eaQueueName);
		} catch (ActiveMQException e) {
			throw new RuntimeException("Failed to create producer", e);
		}

		this.messageFactory = new MessageFactory(transactedSession);

		this.transactionManager = transactionManager;

		this.pendingMessages = metrics.counter(METRIC_NAME_PENDING_MESSAGES);
		this.pendingMessagesMissing = metrics.counter(METRIC_NAME_PENDING_MESSAGES_MISSING);
		this.processedImages = metrics.meter(METRIC_NAME_PROCESSED_IMAGES);
	}

	/**
	 * Create an instance to manage queue to database transactions. Uses the default queues.
	 * 
	 * @param transactedSession
	 *            session with transactions enabled
	 * @param transactionManager
	 *            for transacted database operations
	 * @param pendingRepository
	 *            repository for storing pending image records
	 * @param imageRepository
	 *            repository for storing results
	 * @param metrics
	 *            for tracking metrics
	 */
	@Inject
	public QueueToDatabaseTransaction(@Named("transacted") ClientSession transactedSession,
			TransactionManager transactionManager, PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository, MetricRegistry metrics) {
		this(transactedSession, transactionManager, QueueAddress.EA_UPDATE.toString(),
				pendingRepository, imageRepository, metrics);
	}

	private boolean isTransactedSession(ClientSession toCheck) {
		return !(toCheck.isAutoCommitAcks() || toCheck.isAutoCommitSends());
	}

	/**
	 * Drain the collected messages and store them in the database. This is done in a transaction, both message queue
	 * and database will be rolled back if there is an error.
	 * 
	 * @param messages
	 *            to store
	 */
	@Override
	public void onDrain(List<ClientMessage> messages) {
		try {
			transactionManager.callInTransaction(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					onCall(messages);
					return null;
				}
			});
		} catch (SQLException e) {
			LOGGER.warn("Failed to store {} messages: {}", messages.size(), e.toString(), e.getCause());

			try {
				session.rollback();
			} catch (ActiveMQException mqException) {
				String message = "Failed to rollback message transaction";
				LOGGER.error(message, mqException);
				throw new RuntimeException(message, mqException);
			}
		}
	}

	/**
	 * Called on a database transaction. This method is protected for testing purposes.
	 * 
	 * @param messages
	 *            to store in the database
	 * @throws RepositoryException
	 *             if there is an error with the database
	 * 
	 * @throws ActiveMQException
	 *             if there is an error with the message queue
	 */
	protected void onCall(List<ClientMessage> messages) throws RepositoryException, ActiveMQException {
		for (ClientMessage message : messages) {
			processMessage(message);
			message.acknowledge();
		}

		session.commit();
	}

	private void processMessage(ClientMessage message) throws RepositoryException {
			long most = message.getBodyBuffer().readLong();
			long least = message.getBodyBuffer().readLong();
			long hash = message.getBodyBuffer().readLong();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Received result message with id {} and hash {}", new UUID(most, least), hash);
			}

			PendingHashImage pending = pendingRepository.getByUUID(most, least);

			if (pending != null) {
				pendingMessages.dec();
				processedImages.mark();
				updateRecords(hash, pending);
			} else {
				pendingMessagesMissing.inc();
				LOGGER.warn("No pending hash record found for {}", new UUID(most, least));
			}
		}

		private void updateRecords(long hash, PendingHashImage pending) throws RepositoryException {
			storeHash(pending.getPathAsPath(), hash);
			pendingRepository.remove(pending);
			ClientMessage eaUpdate = messageFactory.eaUpdate(pending.getPathAsPath(), hash);

			try {
				producer.send(eaUpdate);
				LOGGER.trace("Sent EA update for {} to address {}", pending.getPath(), producer.getAddress());
			} catch (ActiveMQException e) {
				LOGGER.warn("Failed to send ea update message for {}: {}", pending.getPath(), e.toString());
			}
		}

		private void storeHash(Path path, long hash) throws RepositoryException {
			LOGGER.trace("Creating record for {} with hash {}", path, hash);
			imageRepository.store(new ImageRecord(path.toString(), hash));
		}
}
