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

import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

public class TaskMessageHandler implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageHandler.class);
	
	public static final String METRIC_NAME_PENDING_MESSAGES = MetricRegistry.name(TaskMessageHandler.class, "pending",
			"messages");

	private final PendingHashImageRepository pendingRepository;
	private final Counter pendingMessages;

	/**
	 * Create a handler for Task messages, using the default address for extended attribute update messages.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param metrics
	 *            registry for tracking metrics
	 */
	@Inject
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			MetricRegistry metrics) {
		this(pendingRepository, imageRepository, null, QueueAddress.EA_UPDATE.toString(), metrics);
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
	 * @deprecated Use
	 *             {@link TaskMessageHandler#TaskMessageHandler(PendingHashImageRepository, ImageRepository, MetricRegistry)}
	 *             instead.
	 */
	@Deprecated
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			ClientSession session, MetricRegistry metrics) {
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
	 * @deprecated Use
	 *             {@link TaskMessageHandler#TaskMessageHandler(PendingHashImageRepository, ImageRepository, ClientSession, MetricRegistry)}
	 *             instead.
	 */
	@Deprecated
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			ClientSession session, String eaUpdateAddress, MetricRegistry metrics) {
		this.pendingRepository = pendingRepository;
		this.pendingMessages = metrics.counter(METRIC_NAME_PENDING_MESSAGES);
	}

	/**
	 * Create a handler for Task messages.
	 * 
	 * @param pendingRepository
	 *            repository for pending messages
	 * @param imageRepository
	 *            repository for hashed images
	 * @param eaUpdateAddress
	 *            address for sending extended attribute updates
	 * @param metrics
	 *            registry for tracking metrics
	 */
	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			String eaUpdateAddress, MetricRegistry metrics) {
		this.pendingRepository = pendingRepository;
		this.pendingMessages = metrics.counter(METRIC_NAME_PENDING_MESSAGES);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage msg) {
		try {
			if (isTaskType(msg, TaskType.track)) {
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
			String cause = "unknown";
			
			if(e.getCause() != null) {
				cause = e.getCause().toString();
			}
			
			LOGGER.warn("Failed to store result message: {}, cause:{}", e.toString(), cause);
		}
	}

	private boolean isTaskType(ClientMessage message, TaskType task) {
		return task.toString().equals(message.getStringProperty(MessageProperty.task.toString()));
	}
}
