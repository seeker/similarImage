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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.util.ImageUtil;
import com.github.dozedoff.similarImage.util.MessagingUtil;
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
public class ResizerNode implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResizerNode.class);

	private static final String DUPLICATE_MESSAGE = "Image {} is already in the hashing queue, discarding";
	private static final String DUMMY = "";

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ClientSession session;
	private final ImageResizer resizer;
	private MessageFactory messageFactory;

	private final Cache<String, String> pendingCache;

	/**
	 * Create a new consumer for hash messages. Uses the default addresses for queues.
	 * 
	 * @param session
	 *            to talk to the server
	 * @param resizer
	 *            for resizing images
	 * @throws Exception
	 *             if the setup for {@link QueryMessage} failed
	 */
	public ResizerNode(ClientSession session, ImageResizer resizer) throws Exception {

		this(session, resizer, QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.HASH_REQUEST.toString(),
				new QueryMessage(session, QueueAddress.REPOSITORY_QUERY));
	}

	/**
	 * Create a new consumer for hash messages. Uses the default address for repository queries.
	 * 
	 * @param session
	 *            to talk to the server
	 * @param resizer
	 *            for resizing images
	 * @param inAddress
	 *            for hashes
	 * @param outAddress
	 *            for result messages
	 * @throws Exception
	 *             if the setup for {@link QueryMessage} failed
	 */
	public ResizerNode(ClientSession session, ImageResizer resizer, String inAddress, String outAddress) throws Exception {

		this(session, resizer, inAddress, outAddress, new QueryMessage(session, QueueAddress.REPOSITORY_QUERY));
	}

	/**
	 * Create a new consumer for hash messages. Uses the default address for repository queries. <b>For testing only!</b>
	 * 
	 * @param session
	 *            to talk to the server
	 * @param resizer
	 *            for resizing images
	 * @param requestAddress
	 *            for hashes
	 * @param resultAddress
	 *            for result messages
	 * @param queryMessage
	 *            instance to use for repository queries
	 * @throws Exception
	 *             if the setup for {@link QueryMessage} failed
	 */
	protected ResizerNode(ClientSession session, ImageResizer resizer, String requestAddress, String resultAddress,
			QueryMessage queryMessage) throws Exception {
		// TODO replace with list of pending files
		this.session = session;
		this.consumer = session.createConsumer(requestAddress);
		this.producer = session.createProducer(resultAddress);
		this.resizer = resizer;
		this.messageFactory = new MessageFactory(session);
		this.pendingCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

		this.consumer.setMessageHandler(this);

		preLoadCache(queryMessage);
	}

	private void preLoadCache(QueryMessage queryMessage) throws Exception {
		List<String> pending = queryMessage.pendingImagePaths();
		LOGGER.info("Pre loading cache with {} pending paths", pending.size());
		for (String path : pending) {
			pendingCache.put(path, DUMMY);
		}
	}

	/**
	 * Overwrite the {@link MessageFactory} of the class. For testing only!
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

		try {
			pathPropterty = message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY);
			LOGGER.debug("Resize request for image {}", pathPropterty);

			if (pendingCache.getIfPresent(pathPropterty) != null) {
				LOGGER.trace("{} found in cache, skipping...", pathPropterty);
				return;
			}

			Path path = Paths.get(pathPropterty);
			ByteBuffer buffer = ByteBuffer.allocate(message.getBodySize());
			message.getBodyBuffer().readBytes(buffer);

			Path filename = path.getFileName();
			InputStream is = new ByteArrayInputStream(buffer.array());

			if (filename != null && filename.toString().toLowerCase().endsWith(".gif")) {
				GifImage gi = GifDecoder.read(is);
				is = new ByteArrayInputStream(ImageUtil.imageToBytes(gi.getFrame(0)));
			}

			byte[] resizedImageData = resizer.resize(is);

			UUID uuid = UUID.randomUUID();
			ClientMessage trackMessage = messageFactory.trackPath(path, uuid);
			producer.send(QueueAddress.RESULT.toString(), trackMessage);
			LOGGER.trace("Sent tracking message for {} with UUID {}", pathPropterty, uuid);

			ClientMessage response = messageFactory.hashRequestMessage(resizedImageData, uuid);

			LOGGER.trace("Sending hash request with id {} instead of path {}", uuid, path);
			producer.send(response);
			pendingCache.put(pathPropterty, DUMMY);
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
		} catch (Exception e) {
			LOGGER.error("Unhandled exception: {}", e.toString());
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
		ClientMessage response = messageFactory.corruptMessage(Paths.get(path));
		producer.send(response);
	}

	/**
	 * Stop this consumer and clean up resources.
	 */
	public void stop() {
		LOGGER.info("Stopping {}...", this.getClass().getSimpleName());
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(producer);
		MessagingUtil.silentClose(session);
	}
}
