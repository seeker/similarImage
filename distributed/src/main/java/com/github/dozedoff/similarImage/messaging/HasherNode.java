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
import java.nio.ByteBuffer;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.util.ImageUtil;
import com.github.dozedoff.similarImage.util.MessagingUtil;

/**
 * Consumes resize messages, hashes them and produces result messages.
 * 
 * @author Nicholas Wright
 *
 */
public class HasherNode implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HasherNode.class);

	private final ClientConsumer consumer;
	private final ClientProducer producer;
	private final ClientSession session;
	private final ImagePHash hasher;
	private MessageFactory messageFactory;

	private final Meter hashRequests;

	public static final String METRIC_NAME_HASH_MESSAGES = MetricRegistry.name(HasherNode.class, "hash", "messages");

	/**
	 * Create a hash consumer that listens and responds on the given addresses.
	 * 
	 * @param session
	 *            of the client
	 * @param hasher
	 *            to use for hashing files
	 * @param requestAddress
	 *            to listen to
	 * @param resultAddress
	 *            where to send the results of hashing
	 * @param metrics
	 *            registry for tracking metrics
	 * @throws ActiveMQException
	 *             if there is an error with the queue
	 */
	public HasherNode(ClientSession session, ImagePHash hasher, String requestAddress, String resultAddress,
			MetricRegistry metrics) throws ActiveMQException {
		this.hasher = hasher;
		this.session = session;
		this.consumer = session.createConsumer(requestAddress);
		this.producer = session.createProducer(resultAddress);
		this.consumer.setMessageHandler(this);
		this.messageFactory = new MessageFactory(session);

		this.hashRequests = metrics.meter(METRIC_NAME_HASH_MESSAGES);
	}

	/**
	 * Create a hash consumer that listens and responds on the given addresses.
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
	 * @deprecated Use the constructor with {@link MetricRegistry}
	 */
	@Deprecated
	public HasherNode(ClientSession session, ImagePHash hasher, String requestAddress, String resultAddress) throws ActiveMQException {
		this(session, hasher, requestAddress, resultAddress, new MetricRegistry());
	}

	protected final void setMessageFactory(MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
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
		hashRequests.mark();
		try {
			long most = message.getBodyBuffer().readLong();
			long least = message.getBodyBuffer().readLong();
			ByteBuffer buffer = ByteBuffer.allocate(message.getBodySize());
			message.getBodyBuffer().readBytes(buffer);

			long hash = doHash(ImageUtil.bytesToImage(buffer.array()));
			ClientMessage response = messageFactory.resultMessage(hash, most, least);
			producer.send(response);
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to process message: {}", e.toString());
		} catch (Exception e) {
			LOGGER.error("Failed to process image: {}", e.toString());
		}
	}

	private long doHash(BufferedImage image) throws Exception {
		long hash = hasher.getLongHashScaledImage(image);
		return hash;
	}
}
