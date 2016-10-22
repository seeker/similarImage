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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.IIOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.util.ImageUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import at.dhyan.open_imaging.GifDecoder;
import at.dhyan.open_imaging.GifDecoder.GifImage;

/**
 * Consumes resize request messages with full-sized images and produces hash request messages with a resized image for hashing.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisResizeRequestConsumer implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisResizeRequestConsumer.class);

	private static final int SEND_ACK_CACHE_TIMEOUT = 5;
	private static final AtomicInteger TRACKING_ID_SEQUENCE = new AtomicInteger();
	private static final String DUPLICATE_MESSAGE = "Image {} is already in the hashing queue, discarding";

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ClientSession session;
	private final ImageResizer resizer;
	private final PendingHashImageRepository pendingRepo; // TODO replace with repository messages
	private final Cache<Integer, String> messageSendAcknowledgedCache;
	private MessageFactory messageFactory;

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
	 * @param pendingRepo
	 *            for pending hash requests
	 * @throws ActiveMQException
	 *             if there is an error with the queues
	 */
	public ArtemisResizeRequestConsumer(ClientSession session, ImageResizer resizer, String requestAddress,
			String resultAddress, PendingHashImageRepository pendingRepo)
			throws ActiveMQException {

		this.session = session;
		this.consumer = session.createConsumer(requestAddress);
		this.producer = session.createProducer(resultAddress);
		this.resizer = resizer;
		this.pendingRepo = pendingRepo;
		this.messageFactory = new MessageFactory(session);

		this.consumer.setMessageHandler(this);

		// if we don't hear from the broker in this time, then it won't happen
		this.messageSendAcknowledgedCache = CacheBuilder.newBuilder()
				.expireAfterAccess(SEND_ACK_CACHE_TIMEOUT, TimeUnit.MINUTES).build();
	}

	/**
	 * Overwrite the {@link MessageFactory} of the class.
	 * 
	 * @param messageFactory
	 *            to set
	 */
	protected final void setMessageFactory(MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage message) {
		String pathPropterty = null;
		int trackingId = -1;

		try {
			pathPropterty = message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY);
			LOGGER.debug("Resize request for image {}", pathPropterty);

			Path path = Paths.get(pathPropterty);
			if (pendingRepo.exists(new PendingHashImage(path))) {
				LOGGER.info(DUPLICATE_MESSAGE, path);
				return;
			}

			ByteBuffer buffer = ByteBuffer.allocate(message.getBodySize());
			message.getBodyBuffer().readBytes(buffer);

			Path filename = path.getFileName();
			InputStream is = new ByteArrayInputStream(buffer.array());
			
			if (filename != null && filename.toString().toLowerCase().endsWith(".gif")) {
				GifImage gi = GifDecoder.read(is);
				is = new ByteArrayInputStream(ImageUtil.imageToBytes(gi.getFrame(0)));
			}
			
			byte[] resizedImageData = resizer.resize(is);



			PendingHashImage pending = new PendingHashImage(path);

			if (pendingRepo.store(pending)) {
				trackingId = pending.getId();
				ClientMessage response = messageFactory.hashRequestMessage(resizedImageData, trackingId);

				LOGGER.debug("Sending hash request with id {} instead of path {}", trackingId, path);
				producer.send(response);
				messageSendAcknowledgedCache.put(trackingId, pathPropterty);
				return; // FIXME ugly hack to remove pending records in case of failure
			} else {
				LOGGER.warn(DUPLICATE_MESSAGE, path);
			}
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to send message: {}", e.toString());
		} catch (IIOException | ArrayIndexOutOfBoundsException ie) {
			markImageCorrupt(pathPropterty);
		} catch (IOException e) {
			if (isImageError(e.getMessage())) {
				markImageCorrupt(pathPropterty);
			} else {
				LOGGER.error("Failed to process image: {}", e.toString());
			}
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to store pending image entry: {}, cause: {}", e.toString(), e.getCause().getMessage());
		}

		try {
			pendingRepo.removeById(trackingId);// hack hack
		} catch (RepositoryException e) {
			LOGGER.error("Failed to remove pending image with id {} after failure: {}", trackingId, e.toString());
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
