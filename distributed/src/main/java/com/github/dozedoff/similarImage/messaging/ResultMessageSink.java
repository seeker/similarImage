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

import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

/**
 * Consumes {@link ClientMessage}s and stores them in a {@link MessageCollector}.
 */
public class ResultMessageSink implements Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResultMessageSink.class);

	private static final long DEFAULT_MESSAGE_DRAIN_INTERVAL = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
	private static final String WORKER_THREAD_NAME = "Message Sink Worker";

	private final ClientConsumer consumer;
	private final MessageCollector collector;
	private final long messageIdleTimeout;
	private final Sink sink;

	/**
	 * Create a {@link ResultMessageSink} that collects {@link ClientMessage}s and stores them in a
	 * {@link MessageCollector}.
	 * 
	 * @param transactedSession
	 *            to use for accessing the messages
	 * @param collector
	 *            for storing the messages
	 * @param resultQueueName
	 *            queue to query for messages
	 * @param messageIdleTimeout
	 *            if no message is received for this long, the collector will be drained
	 * @throws ActiveMQException
	 *             if there is an error accessing the queue
	 */
	public ResultMessageSink(ClientSession transactedSession, MessageCollector collector, String resultQueueName,
			long messageIdleTimeout) throws ActiveMQException {

		if (!isTransactedSession(transactedSession)) {
			throw new IllegalArgumentException("Session must be transactional");
		}

		this.consumer = transactedSession.createConsumer(resultQueueName,
				MessageProperty.task.toString() + " IS NOT NULL AND "
				+ MessageProperty.task.toString() + " NOT IN ('" + TaskType.track.toString() + "')");
		this.collector = collector;
		this.messageIdleTimeout = messageIdleTimeout;
		this.sink = new Sink();

		sink.start();
	}

	private boolean isTransactedSession(ClientSession toCheck) {
		return !(toCheck.isAutoCommitAcks() || toCheck.isAutoCommitSends());
	}

	/**
	 * Create a {@link ResultMessageSink} that collects {@link ClientMessage}s and stores them in a
	 * {@link MessageCollector}. Uses the default queue name and message idle timeout.
	 * 
	 * @param transactedSession
	 *            to use for accessing the messages
	 * @param collector
	 *            for storing the messages
	 * @throws ActiveMQException
	 *             if there is an error accessing the queue
	 */
	public ResultMessageSink(ClientSession transactedSession, MessageCollector collector) throws ActiveMQException {
		this(transactedSession, collector, QueueAddress.RESULT.toString(), DEFAULT_MESSAGE_DRAIN_INTERVAL);
	}

	private class Sink extends Thread {
		Sink() {
			super(WORKER_THREAD_NAME);
			this.setDaemon(true);
		}

		@Override
		public void run() {
			LOGGER.info("Starting {}", WORKER_THREAD_NAME);
			while (!isInterrupted()) {
				try {
					ClientMessage message = consumer.receive(messageIdleTimeout);
					if (message == null) {
						collector.drain();
					} else {
						LOGGER.trace("Collecting message {}", message.toString());
						collector.addMessage(message);
					}
				} catch (ActiveMQException e) {
					LOGGER.warn("Failed to consume message: {}", e.toString());
				}
			}

			collector.drain();
			LOGGER.debug("{} interrupted", WORKER_THREAD_NAME);
		}
	}

	/**
	 * Stop the sink worker thread and close the consumer.
	 */
	@Override
	public void stop() {
		LOGGER.info("Stopping {}", WORKER_THREAD_NAME);
		sink.interrupt();
		try {
			sink.join();
		} catch (InterruptedException ie) {
			LOGGER.warn("Interrupted while wating for sink to terminate: {}", ie.toString());
		}

		try {
			consumer.close();
		} catch (ActiveMQException e) {
			LOGGER.warn("Failed to close consumer: {}", e.toString());
		}
	}
}
