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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.QueryType;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;
import com.github.dozedoff.similarImage.util.MessagingUtil;

/**
 * This node connects to repositories. Used for storing hash results and answering query messages.
 */
public class RepositoryNode implements MessageHandler, Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryNode.class);

	private static final String REPOSITORY_ERROR_MESSAGE = "Failed to access repository:{}, cause:{}";
	private static final String RESPONSE_SEND_ERROR = "Failed to send response message: {}";

	private final ClientConsumer consumer;
	private final ClientConsumer taskConsumer;
	private final ClientProducer producer;
	private final PendingHashImageRepository pendingRepository;
	private final MessageFactory messageFactory;

	// TODO remove metrics parameter from constructor
	// TODO remove image repository parameter from constructor

	/**
	 * Create a instance using the given instance and repository
	 * 
	 * @param session
	 *            to use for messages
	 * @param queryAddress
	 *            to use for listening to queries
	 * @param taskAddress
	 *            address for listening to tasks
	 * @param pendingRepository
	 *            for pending file queries
	 * @param imageRepository
	 *            for storing hash results
	 * @param taskMessageHandler
	 *            handler to use for task messages
	 * @param metrics
	 *            registry for tracking metrics
	 */
	public RepositoryNode(ClientSession session, String queryAddress, String taskAddress,
			PendingHashImageRepository pendingRepository, ImageRepository imageRepository,
			TaskMessageHandler taskMessageHandler, MetricRegistry metrics) {

		try {
			this.consumer = session.createConsumer(queryAddress);

			this.taskConsumer = session.createConsumer(taskAddress,
					MessageProperty.task.toString() + " IS NOT NULL AND " + MessageProperty.task.toString()
							+ " NOT IN ('" + TaskType.result.toString() + "')");
			this.taskConsumer.setMessageHandler(taskMessageHandler);
			this.producer = session.createProducer();
			this.pendingRepository = pendingRepository;
			this.consumer.setMessageHandler(this);
			messageFactory = new MessageFactory(session);
			LOGGER.info("Listening to request messages on {} ...", queryAddress);
		} catch (ActiveMQException e) {
			throw new RuntimeException("Failed to create " + RepositoryNode.class.getSimpleName(), e);
		}
	}

	/**
	 * Create a instance using the given instance and repository. The default address is used to listen for queries.
	 * 
	 * @param session
	 *            to use for messages
	 * @param pendingRepository
	 *            for pending file queries
	 * @param taskMessageHandler
	 *            handler to use for task messages
	 * @param metrics
	 *            registry for tracking metrics
	 */
	@Inject
	public RepositoryNode(@Named("normal") ClientSession session, PendingHashImageRepository pendingRepository,
			TaskMessageHandler taskMessageHandler, MetricRegistry metrics) {
		this(session, QueueAddress.REPOSITORY_QUERY.toString(), QueueAddress.RESULT.toString(), pendingRepository, null,
				taskMessageHandler, metrics);
	}

	private String getReplyReturnAddress(ClientMessage message) {
		return message.getStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME);
	}

	private boolean isQueryType(String messageValue, QueryType queryType) {
		return queryType.toString().equals(messageValue);
	}

	/**
	 * Accept messages and check if they are query messages, if so handle them.
	 * 
	 * @param message
	 *            recieved message
	 */
	@Override
	public void onMessage(ClientMessage message) {
		if (message.containsProperty(MessageProperty.repository_query.toString())) {
			String queryType = message.getStringProperty(MessageProperty.repository_query.toString());
			LOGGER.debug("Got query message: {}", queryType);

			if (isQueryType(queryType, QueryType.pending)) {
				LOGGER.debug("Query for pending files");
				try {
					ClientMessage response = messageFactory.pendingImageResponse(pendingRepository.getAll());
					producer.send(getReplyReturnAddress(message), response);
					LOGGER.debug("Sent pending query response message");
				} catch (IOException e) {
					LOGGER.error("Failed to add paths to message:", e.toString());
				} catch (RepositoryException e) {
					LOGGER.error(REPOSITORY_ERROR_MESSAGE, e.toString(), e.getCause().getMessage());
				} catch (ActiveMQException e) {
					LOGGER.error(RESPONSE_SEND_ERROR, e.toString());
				}
			} else if (isQueryType(queryType, QueryType.TRACK)) {
				String errorMessage = "Tracking id requests are no longer supported!";
				LOGGER.error(errorMessage);
				throw new UnsupportedOperationException(errorMessage);
			} else {
				LOGGER.error("Unhandled query request: {}", queryType);
			}
		} else {
			LOGGER.warn("Received non query message. Properties: {}", message.getPropertyNames());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping {}...", this.toString());
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(taskConsumer);
		MessagingUtil.silentClose(producer);
	}

	/**
	 * Returns the class name.
	 * 
	 * @return the name of this class
	 */
	@Override
	public String toString() {
		return RepositoryNode.class.getSimpleName();
	}
}
