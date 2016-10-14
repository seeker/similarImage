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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;

public class ArtemisResultConsumer extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisResultConsumer.class);

	private static final long RECEIVE_TIMEOUT_MILLI = 500;

	private final ImageRepository imageRepository;
	private final ClientConsumer consumer;

	public ArtemisResultConsumer(ClientSession session, ImageRepository imageRepository) throws ActiveMQException {
		this.imageRepository = imageRepository;
		this.consumer = session.createConsumer(ArtemisSession.ADDRESS_RESULT_QUEUE);
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
			ClientMessage msg = consumer.receive(RECEIVE_TIMEOUT_MILLI);

			if (msg == null) {
				continue;
			}

			imageRepository.store(new ImageRecord(msg.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY),
					msg.getLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY)));
			} catch (RepositoryException | ActiveMQException e) {
				LOGGER.error("Failed to store result message: {}", e.toString());
			}
		}
	}
}
