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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.IIOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.util.MessagingUtil;

import at.dhyan.open_imaging.GifDecoder;
import at.dhyan.open_imaging.GifDecoder.GifImage;

/**
 * Consumes hash request messages and produces result messages.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisHashConsumer implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisHashConsumer.class);

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ClientSession session;
	private final ImagePHash hasher;


	/**
	 * Create a hash consumer that listens to the given address.
	 * 
	 * @param session
	 *            of the client
	 * @param hasher
	 *            to use for hashing files
	 * @param requestAddress
	 *            to listen to
	 * @param resultAddress
	 *            where to send the results of hashing
	 * @throws ActiveMQException
	 *             if there is an error with the queue
	 */
	public ArtemisHashConsumer(ClientSession session, ImagePHash hasher, String requestAddress, String resultAddress)
			throws ActiveMQException {
		this.hasher = hasher;
		this.session = session;
		this.consumer = session.createConsumer(requestAddress);
		this.producer = session.createProducer(resultAddress);
		this.consumer.setMessageHandler(this);
	}

	/**
	 * Stops this consumer
	 */
	public void stop() {
		LOGGER.info("Stopping {}...", this.getClass().getSimpleName());
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(producer);
		MessagingUtil.silentClose(session);
	}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onMessage(ClientMessage message) {
			
			Path path =null;
			try {
				path= Paths.get(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY));
				ByteBuffer buffer = ByteBuffer.allocate(message.getBodySize());
				message.getBodyBuffer().readBytes(buffer);
			
				long hash = processFile(path, new ByteArrayInputStream(buffer.array()));
				ClientMessage response = session.createMessage(false);
				response.putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, path.toString());
				response.putLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY, hash);
			
				producer.send(response);
				LOGGER.debug("Sent hash response message for {}", path);
			} catch (ActiveMQException e) {
				LOGGER.error("Failed to process message: {}", e.toString());
			}catch (InvalidPathException e) {
				LOGGER.error("File path was invalid: {}", e.toString());
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

	private void markImageCorrupt(Path path) {
		LOGGER.warn("Unable to read image {}, marking as corrupt", path);
		try {
			sendImageErrorResponse(path);
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to send corrupt image message: {}", e.toString());
		}
	}

		private void sendImageErrorResponse(Path path) throws ActiveMQException {
			ClientMessage response = session.createMessage(false);
			response.putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, path.toString());
			response.putStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY, ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT);
			producer.send(response);
		}

		private long processFile(Path next, InputStream is) throws IOException {
			Path filename = next.getFileName();

			if (filename != null && filename.toString().toLowerCase().endsWith(".gif")) {
				GifImage gi = GifDecoder.read(is);

				long hash = hasher.getLongHash(gi.getFrame(0));
				return hash;
			} else {
				return doHash(next, is);
			}
		}

		private long doHash(Path next, InputStream is) throws IOException {
			long hash = hasher.getLongHash(is);
			return hash;
		}
}
