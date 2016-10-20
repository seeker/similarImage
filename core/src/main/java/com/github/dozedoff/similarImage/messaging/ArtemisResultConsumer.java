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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.io.ExtendedAttribute;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.util.MessagingUtil;

/**
 * Consumes result messages and updates the {@link ImageRepository} and extended attributes.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisResultConsumer implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisResultConsumer.class);

	private final ImageRepository imageRepository;
	private final ClientConsumer consumer;
	private final ExtendedAttributeQuery eaQuery;
	private final HashAttribute hashAttribute;
	private final ClientSession session;
	public static final String CORRUPT_EA_NAMESPACE = ExtendedAttribute.createName("corrupt");

	// TODO rewrite like handler
	public ArtemisResultConsumer(ClientSession session, ImageRepository imageRepository, ExtendedAttributeQuery eaQuery,
			HashAttribute hashAttribute)
			throws ActiveMQException {
		this.imageRepository = imageRepository;
		this.session = session;
		// TODO move address into constructor
		this.consumer = session.createConsumer(QueueAddress.RESULT.toString());
		this.eaQuery = eaQuery;
		this.hashAttribute = hashAttribute;
		this.consumer.setMessageHandler(this);
	}

	/**
	 * Stops this consumer
	 */
	public void stop() {
		LOGGER.info("Stopping {}...", this.getClass().getSimpleName());
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(session);
	}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onMessage(ClientMessage msg) {
			try {
				Path path = Paths.get(msg.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY));


				if (msg.containsProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY)) {
					long hash = msg.getLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY);
					storeHash(path,hash);
				} else if (ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT
						.equals(msg.getStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY))) {
					markAsCorrupt(path);
				} else {
					LOGGER.error("Unhandled message: {}", msg);
				}
			} catch (RepositoryException e) {
				LOGGER.warn("Failed to store result message: {}", e.toString());
			}
		}

		private void markAsCorrupt(Path path) {
			if (eaQuery.isEaSupported(path)) {
				try {
					hashAttribute.markCorrupted(path);
				} catch (IOException e) {
					LOGGER.warn("Failed to mark {} as corrupt: {}", path, e.toString());
				}
			}
		}

		private void storeHash(Path path, long hash) throws RepositoryException {
			LOGGER.debug("Creating record for {} with hash {}", path, hash);
			imageRepository.store(new ImageRecord(path.toString(), hash));

			if (eaQuery.isEaSupported(path)) {
				hashAttribute.writeHash(path, hash);
			}
		}
}
