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

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.MessageFactory.QueryType;

public class QueryResponder implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryResponder.class);

	private static final String REPOSITORY_ERROR_MESSAGE = "Failed to access repository:{}, cause:{}";
	private static final String RESPONSE_SEND_ERROR = "Failed to send response message: {}";

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final PendingHashImageRepository pendingRepository;
	private final MessageFactory messageFactory;

	public QueryResponder(ClientSession session, String queryAddress, PendingHashImageRepository pendingRepository)
			throws ActiveMQException {

		this.consumer = session.createConsumer(queryAddress);
		this.producer = session.createProducer();
		this.pendingRepository = pendingRepository;
		this.consumer.setMessageHandler(this);
		messageFactory = new MessageFactory(session);
		LOGGER.info("Listening to request messages on {} ...", queryAddress);
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
				LOGGER.trace("Query for tracking id");
				try {
					PendingHashImage pending = new PendingHashImage(message.getBodyBuffer().readString());
					pendingRepository.store(pending);
					int pendingId = pending.getId();
					ClientMessage response = messageFactory.trackPathResponse(pendingId);
					producer.send(getReplyReturnAddress(message), response);
					LOGGER.trace("Sent tracking id query response with id {}", pendingId);
				} catch (RepositoryException e) {
					LOGGER.error(REPOSITORY_ERROR_MESSAGE, e.toString(), e.getCause().getMessage());
				} catch (ActiveMQException e) {
					LOGGER.error(RESPONSE_SEND_ERROR, e.toString());
				}
			} else {
				LOGGER.error("Unhandled query request: {}", queryType);
			}
		} else {
			LOGGER.warn("Received non query message. Properties: {}", message.getPropertyNames());
		}
	}
}
