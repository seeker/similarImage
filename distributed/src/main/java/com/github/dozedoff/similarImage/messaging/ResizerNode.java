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

import java.awt.image.BufferedImage;
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
import javax.imageio.ImageIO;
import javax.inject.Inject;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.github.dozedoff.similarImage.image.ImageResizer;
import com.github.dozedoff.similarImage.io.ByteBufferInputstream;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
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
public class ResizerNode implements MessageHandler, Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResizerNode.class);

	private static final String DUMMY = "";
	private static final int INITIAL_BUFFER_SIZE = 1024 * 1024 * 5;
	private static final int PENDING_CACHE_TIMEOUT_MINUTES = 5;

	private static final String NAME_PENDING_CACHE = "pendingCache";
	private static final String NAME_RESIZE = "resize";

	public static final String METRIC_NAME_RESIZE_MESSAGES = MetricRegistry.name(ResizerNode.class, NAME_RESIZE,
			"messages");
	public static final String METRIC_NAME_RESIZE_DURATION = MetricRegistry.name(ResizerNode.class, NAME_RESIZE, "duration");
	public static final String METRIC_NAME_PENDING_CACHE_HIT = MetricRegistry.name(ResizerNode.class, NAME_PENDING_CACHE,
			"hit");
	public static final String METRIC_NAME_PENDING_CACHE_MISS = MetricRegistry.name(ResizerNode.class, NAME_PENDING_CACHE,
			"miss");
	public static final String METRIC_NAME_IMAGE_SIZE = MetricRegistry.name(ResizerNode.class, NAME_RESIZE, "imageSize");
	public static final String METRIC_NAME_BUFFER_RESIZE = MetricRegistry.name(ResizerNode.class, "buffer", NAME_RESIZE);

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ImageResizer resizer;
	private MessageFactory messageFactory;

	private final Cache<String, String> pendingCache;
	private ByteBuffer messageBuffer;
	private final Meter resizeRequests;
	private final Meter pendingCacheHit;
	private final Meter pendingCacheMiss;
	private final Histogram imageSize;
	private final Counter bufferResize;
	private final Timer resizeDuration;
	private final UUID identity;

	/**
	 * Create a new consumer for hash messages. Uses the default addresses for queues.
	 * 
	 * @param session
	 *            to talk to the server
	 * @param resizer
	 *            for resizing images
	 * @param metrics
	 *            registry for tracking metrics
	 * @throws Exception
	 *             if the setup for {@link QueryMessage} failed
	 */
	@Inject
	public ResizerNode(ClientSession session, ImageResizer resizer, MetricRegistry metrics) {
		this(session, resizer, QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.HASH_REQUEST.toString(),
				new QueryMessage(session, QueueAddress.REPOSITORY_QUERY), metrics);
	}

	/**
	 * Create a new consumer for hash messages. Uses the default address for repository queries. <b>For testing
	 * only!</b>
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
	 * @param metrics
	 *            registry for tracking metrics
	 */
	protected ResizerNode(ClientSession session, ImageResizer resizer, String requestAddress, String resultAddress,
			QueryMessage queryMessage, MetricRegistry metrics) {
		// TODO replace with list of pending files
		try {
			this.consumer = session.createConsumer(requestAddress);
			this.producer = session.createProducer(resultAddress);
			this.resizer = resizer;
			this.messageFactory = new MessageFactory(session);
			this.pendingCache = CacheBuilder.newBuilder()
					.expireAfterAccess(PENDING_CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES).build();

			this.resizeRequests = metrics.meter(METRIC_NAME_RESIZE_MESSAGES);
			this.resizeDuration = metrics.timer(METRIC_NAME_RESIZE_DURATION);
			this.pendingCacheHit = metrics.meter(METRIC_NAME_PENDING_CACHE_HIT);
			this.pendingCacheMiss = metrics.meter(METRIC_NAME_PENDING_CACHE_MISS);
			this.imageSize = metrics.histogram(METRIC_NAME_IMAGE_SIZE);
			this.bufferResize = metrics.counter(METRIC_NAME_BUFFER_RESIZE);

			this.consumer.setMessageHandler(this);
			this.messageBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
			this.identity = UUID.randomUUID();

			LOGGER.debug("Starting {}", this.toString());

			preLoadCache(queryMessage);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create " + ResizerNode.class.getSimpleName(), e);
		}
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

	private void checkBufferCapacity(int messageSize) {
		if (messageSize > messageBuffer.capacity()) {
			bufferResize.inc();
			int oldBufferCap = messageBuffer.capacity();
			allocateNewBuffer(messageSize);
			int newBufferCap = messageBuffer.capacity();
			LOGGER.debug("Message size of {} exceeds buffer capacity of {}, allocated new buffer with capactiy {}", messageSize,
					oldBufferCap, newBufferCap);
		}
	}

	/**
	 * Allocate a new message buffer.
	 * 
	 * @param messageSize
	 *            the size of the message
	 */
	protected void allocateNewBuffer(int messageSize) {
		messageBuffer = ByteBuffer.allocateDirect(calcNewBufferSize(messageSize));
	}
	
	private int calcNewBufferSize(int messageSize) {
		return messageSize * 2;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage message) {
		String pathPropterty = null;
		resizeRequests.mark();
		Context resizeTimeContext = resizeDuration.time();

		try {
			pathPropterty = message.getStringProperty(MessageProperty.path.toString());
			LOGGER.debug("Resize request for image {}", pathPropterty);

			if (pendingCache.getIfPresent(pathPropterty) != null) {
				LOGGER.trace("{} found in cache, skipping...", pathPropterty);
				pendingCacheHit.mark();
				return;
			} else {
				pendingCacheMiss.mark();
			}


			checkBufferCapacity(message.getBodySize());
			messageBuffer.limit(message.getBodySize());
			imageSize.update(message.getBodySize());
			messageBuffer.rewind();
			message.getBodyBuffer().readBytes(messageBuffer);
			messageBuffer.rewind();

			Path path = Paths.get(pathPropterty);
			Path filename = path.getFileName();
			InputStream is = new ByteBufferInputstream(messageBuffer);

			if (filename != null && filename.toString().toLowerCase().endsWith(".gif")) {
				GifImage gi = GifDecoder.read(is);
				is = new ByteArrayInputStream(ImageUtil.imageToBytes(gi.getFrame(0)));
			}

			BufferedImage originalImage = ImageIO.read(is);
			//FIXME nullcheck if image read failed
			byte[] resizedImageData = resizer.resize(originalImage);

			UUID uuid = UUID.randomUUID();
			ClientMessage trackMessage = messageFactory.trackPath(path, uuid);
			producer.send(QueueAddress.RESULT.toString(), trackMessage);
			LOGGER.trace("Sent tracking message for {} with UUID {}", pathPropterty, uuid);

			ClientMessage response = messageFactory.hashRequestMessage(resizedImageData, uuid);

			LOGGER.trace("Sending hash request with id {} instead of path {}", uuid, path);
			producer.send(response);
			pendingCache.put(pathPropterty, DUMMY);
			resizeTimeContext.stop();
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
			LOGGER.debug("", e);
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
		String corruptMessageAddress = QueueAddress.EA_UPDATE.toString();
		LOGGER.trace("Sending corrupt image message for {} to address {}", path, corruptMessageAddress);
		ClientMessage response = messageFactory.corruptMessage(Paths.get(path));
		producer.send(corruptMessageAddress, response);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping {}...", this.toString());
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(producer);
	}

	/**
	 * Returns the class name.
	 * 
	 * @return the name of this class
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(ResizerNode.class.getSimpleName()).append(" {").append(identity.toString()).append("}");

		return sb.toString();
	}
}
