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

import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;

public class QueryResponder implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryResponder.class);

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final PendingHashImageRepository pendingRepository;
	private final MessageFactory messageFactory;

	public QueryResponder(ClientSession session, String queryAddress, PendingHashImageRepository pendingRepository)
			throws ActiveMQException {
		String messageFilter = MessageFactory.QUERY_PROPERTY_NAME.toString() + " IN ('"
				+ MessageFactory.QUERY_PROPERTY_VALUE_PENDING.toString() + "')";

		this.consumer = session.createConsumer(queryAddress, messageFilter);
		this.producer = session.createProducer();
		this.pendingRepository = pendingRepository;
		this.consumer.setMessageHandler(this);
		messageFactory = new MessageFactory(session);
		LOGGER.info("Listening to request messages on {} ...", queryAddress);
	}

	private String getReplyReturnAddress(ClientMessage message) {
		return message.getStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME);
	}

	@Override
	public void onMessage(ClientMessage message) {
		if (message.containsProperty(MessageFactory.QUERY_PROPERTY_NAME)) {
			String queryType = message.getStringProperty(MessageFactory.QUERY_PROPERTY_NAME);
			LOGGER.debug("Got query message: {}", queryType);

			if (MessageFactory.QUERY_PROPERTY_VALUE_PENDING.equals(queryType)) {
				try {
					ClientMessage response = messageFactory.pendingImageResponse(pendingRepository.getAll());
					producer.send(getReplyReturnAddress(message), response);
					LOGGER.debug("Sent pending query response message");
				} catch (IOException e) {
					LOGGER.error("Failed to add paths to message:", e.toString());
				} catch (RepositoryException e) {
					LOGGER.error("Failed to access repository:{}, cause:{}", e.toString(), e.getCause().getMessage());
				} catch (ActiveMQException e) {
					LOGGER.error("Failed to send response message: {}", e.toString());
				}
			} else {
				LOGGER.error("Unhandled query request: {}", queryType);
			}
		} else {
			LOGGER.warn("Received non query message. Properties: {}", message.getPropertyNames());
		}
	}
}
