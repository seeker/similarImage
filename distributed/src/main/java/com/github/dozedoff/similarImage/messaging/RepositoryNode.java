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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.QueryType;

/**
 * This node connects to repositories. Used for storing hash results and answering query messages.
 */
public class RepositoryNode implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryNode.class);

	private static final String REPOSITORY_ERROR_MESSAGE = "Failed to access repository:{}, cause:{}";
	private static final String RESPONSE_SEND_ERROR = "Failed to send response message: {}";

	private final ClientConsumer consumer;
	private final ClientConsumer taskConsumer;
	private final ClientProducer producer;
	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;
	private final MessageFactory messageFactory;

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
	 * @throws ActiveMQException
	 *             if there is an error setting up messaging
	 */
	public RepositoryNode(ClientSession session, String queryAddress, String taskAddress, PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository) throws ActiveMQException {
		this(session, queryAddress, taskAddress, pendingRepository, imageRepository,
				new TaskMessageHandler(pendingRepository, imageRepository, session));
	}

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
	 * @throws ActiveMQException
	 *             if there is an error setting up messaging
	 */
	public RepositoryNode(ClientSession session, String queryAddress, String taskAddress, PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository, TaskMessageHandler taskMessageHandler) throws ActiveMQException {

		this.consumer = session.createConsumer(queryAddress);
		this.taskConsumer = session.createConsumer(taskAddress, MessageProperty.task.toString() + " IS NOT NULL");
		this.taskConsumer.setMessageHandler(taskMessageHandler);
		this.producer = session.createProducer();
		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
		this.consumer.setMessageHandler(this);
		messageFactory = new MessageFactory(session);
		LOGGER.info("Listening to request messages on {} ...", queryAddress);
	}

	/**
	 * Create a instance using the given instance and repository
	 * 
	 * @param session
	 *            to use for messages
	 * @param queryAddress
	 *            to use for listening to queries
	 * @param pendingRepository
	 *            for pending file queries
	 * @param imageRepository
	 *            for storing hash results
	 * @throws ActiveMQException
	 *             if there is an error setting up messaging
	 * @deprecated Use one of the other constructors
	 */
	@Deprecated
	public RepositoryNode(ClientSession session, String queryAddress, PendingHashImageRepository pendingRepository,
			ImageRepository imageRepository) throws ActiveMQException {

		this(session, queryAddress, QueueAddress.RESULT.toString(), pendingRepository, imageRepository);
	}

	/**
	 * Create a instance using the given instance and repository. The default address is used to listen for queries.
	 * 
	 * @param session
	 *            to use for messages
	 * @param pendingRepository
	 *            for pending file queries
	 * @throws ActiveMQException
	 *             if there is an error setting up messaging
	 */
	public RepositoryNode(ClientSession session, PendingHashImageRepository pendingRepository, ImageRepository imageRepository)
			throws ActiveMQException {
		this(session, QueueAddress.REPOSITORY_QUERY.toString(), QueueAddress.RESULT.toString(), pendingRepository, imageRepository);
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
		if (message.containsProperty(MessageFactory.QUERY_PROPERTY_NAME)) {
			String queryType = message.getStringProperty(MessageFactory.QUERY_PROPERTY_NAME);
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

}
