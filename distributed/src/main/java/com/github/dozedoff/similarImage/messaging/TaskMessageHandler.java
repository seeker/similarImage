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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

public class TaskMessageHandler implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageHandler.class);
	
	public static final String METRIC_NAME_PENDING_MESSAGES_MISSING = MetricRegistry.name(RepositoryNode.class,
			"pending", "messages", "missing");
	public static final String METRIC_NAME_PENDING_MESSAGES = MetricRegistry.name(TaskMessageHandler.class, "pending",
			"messages");

	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;
	private final ClientProducer producer;
	private final MessageFactory messageFactory;
	private final Counter pendingMessages;
	private final Counter pendingMessagesMissing;



	/**
	 * Create a handler for Task messages. Use the default address for extended attribute updates.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param session
	 *            for communicating with the broker
	 * @throws ActiveMQException
	 *             if there is an error creating the producer
	 * @deprecated Use constructor with {@link MetricRegistry}
	 */
	@Deprecated
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository, ClientSession session)
			throws ActiveMQException {
		this(pendingRepository, imageRepository, session, QueueAddress.EA_UPDATE.toString());
	}

	/**
	 * Create a handler for Task messages. Creates an internal {@link MetricRegistry}.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param session
	 *            for communicating with the broker
	 * @param eaUpdateAddress
	 *            address for sending extended attribute updates
	 * @throws ActiveMQException
	 *             if there is an error creating the producer
	 * @deprecated Use constructor with {@link MetricRegistry}
	 */
	@Deprecated
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository, ClientSession session,
			String eaUpdateAddress) throws ActiveMQException {
		this(pendingRepository, imageRepository, session, eaUpdateAddress, new MetricRegistry());
	}

	/**
	 * Create a handler for Task messages, using the default address for extended attribute update messages.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param session
	 *            for communicating with the broker
	 * @param metrics
	 *            registry for tracking metrics
	 * @throws ActiveMQException
	 *             if there is an error creating the producer
	 */
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			ClientSession session, MetricRegistry metrics) throws ActiveMQException {
		this(pendingRepository, imageRepository, session, QueueAddress.EA_UPDATE.toString(), metrics);
	}

	/**
	 * Create a handler for Task messages.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param session
	 *            for communicating with the broker
	 * @param eaUpdateAddress
	 *            address for sending extended attribute updates
	 * @param metrics
	 *            registry for tracking metrics
	 * @throws ActiveMQException
	 *             if there is an error creating the producer
	 */
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			ClientSession session, String eaUpdateAddress, MetricRegistry metrics) throws ActiveMQException {
		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
		this.producer = session.createProducer(eaUpdateAddress);
		this.messageFactory = new MessageFactory(session);
		this.pendingMessages = metrics.counter(METRIC_NAME_PENDING_MESSAGES);
		this.pendingMessagesMissing = metrics.counter(METRIC_NAME_PENDING_MESSAGES_MISSING);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage msg) {
		try {
			if (isTaskType(msg, TaskType.result)) {
				long most = msg.getBodyBuffer().readLong();
				long least = msg.getBodyBuffer().readLong();
				long hash = msg.getBodyBuffer().readLong();

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Received result message with id {} and hash {}", new UUID(most, least), hash);
				}

				PendingHashImage pending = pendingRepository.getByUUID(most, least);
				if (pending != null) {
					pendingMessages.dec();
					updateRecords(hash, pending);
				} else {
					pendingMessagesMissing.inc();
					LOGGER.warn("No pending hash record found for {}", new UUID(most, least));
				}

			} else if (isTaskType(msg, TaskType.track)) {
				String path = msg.getStringProperty(MessageProperty.path.toString());
				long most = msg.getBodyBuffer().readLong();
				long least = msg.getBodyBuffer().readLong();

				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Tracking new path {} with UUID {}", path, new UUID(most, least));
				}

				pendingMessages.inc();
				pendingRepository.store(new PendingHashImage(path, most, least));
			} else {
				LOGGER.error("Unhandled message: {}", msg);
			}
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to store result message: {}", e.toString());
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

	private boolean isTaskType(ClientMessage message, TaskType task) {
		return task.toString().equals(message.getStringProperty(MessageProperty.task.toString()));
	}
}
