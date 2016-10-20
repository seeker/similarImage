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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.IIOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.image.ImageResizer;

/**
 * Consumes resize request messages with full-sized images and produces hash request messages with a resized image for hashing.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisResizeRequestConsumer implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisResizeRequestConsumer.class);

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ClientSession session;
	private final ImageResizer resizer;

	/**
	 * Create a new consumer for hash messages
	 * 
	 * @param session
	 *            to talk to the server
	 * @param resizer
	 *            for resizing images
	 * @param requestAddress
	 *            for hashes
	 * @param resultAddress
	 *            for result messages
	 * @throws ActiveMQException
	 *             if there is an error with the queues
	 */
	public ArtemisResizeRequestConsumer(ClientSession session, ImageResizer resizer, String requestAddress, String resultAddress)
			throws ActiveMQException {
		this.session = session;
		this.consumer = session.createConsumer(requestAddress);
		this.producer = session.createProducer(resultAddress);
		this.resizer = resizer;

		this.consumer.setMessageHandler(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage message) {
		String path = null;

		try {
			path = message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY);

			ByteBuffer buffer = ByteBuffer.allocate(message.getBodySize());
			message.getBodyBuffer().readBytes(buffer);

			byte[] resizedImageData = resizer.resize(new ByteArrayInputStream(buffer.array()));
			ClientMessage response = session.createMessage(true);
			response.getBodyBuffer().writeBytes(resizedImageData);
			response.putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, path);
			producer.send(response);
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to send message: {}", e.toString());
		} catch (IIOException | ArrayIndexOutOfBoundsException ie) {
			markImageCorrupt(path);
		} catch (IOException e) {
			if (isImageError(e.getMessage())) {
				markImageCorrupt(path);
			} else {
				LOGGER.error("Failed to process image: {}", e.toString());
			}
		}
	}

	private boolean isImageError(String message) {
		return message.startsWith("Unknown block") || message.startsWith("Invalid GIF header");
	}

	private void markImageCorrupt(String path) {
		LOGGER.warn("Unable to read image {}, marking as corrupt", path);
		try {
			sendImageErrorResponse(path);
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to send corrupt image message: {}", e.toString());
		}
	}

	private void sendImageErrorResponse(String path) throws ActiveMQException {
		ClientMessage response = session.createMessage(true);
		response.putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, path);
		response.putStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY, ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT);
		producer.send(response);
	}
}
