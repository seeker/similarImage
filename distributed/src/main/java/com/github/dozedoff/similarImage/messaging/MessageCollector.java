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

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Collects {@link ClientMessage}s and drains them to consumers when a set
 * amount of messages has been collected.
 */
public class MessageCollector {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageCollector.class);

	private final int collectedMessageThreshold;
	private final CollectedMessageConsumer[] consumers;
	private final List<ClientMessage> collectedMessages;

	/**
	 * Create a {@link MessageCollector} with the given threshold and consumers
	 * to drain the messages to.
	 * 
	 * @param collectedMessageThreshold
	 *            drain the collected messages to the consumers if the count
	 *            reaches or exceeds the threshold.
	 * @param consumers
	 *            for the messages
	 */
	public MessageCollector(int collectedMessageThreshold, CollectedMessageConsumer... consumers) {
		this.collectedMessageThreshold = collectedMessageThreshold;
		this.consumers = consumers;
		this.collectedMessages = new ArrayList<ClientMessage>();
	}

	/**
	 * Collect a {@link ClientMessage}. If the collected amount reaches the
	 * threshold, a drain is triggered.
	 * 
	 * @param message
	 *            to collect
	 */
	public void addMessage(ClientMessage message) {
		synchronized (collectedMessages) {
			collectedMessages.add(message);
		}

		if (collectedMessages.size() >= collectedMessageThreshold) {
			onDrain();
		}
	}

	/**
	 * Drain all collected {@link ClientMessage}s to the consumers.
	 */
	public void drain() {
		onDrain();
	}

	private void onDrain() {
		ImmutableList<ClientMessage> immutable;
		LOGGER.trace("Draining {} collected messsages...", collectedMessages.size());

		synchronized (collectedMessages) {
			immutable = ImmutableList.copyOf(collectedMessages);
			collectedMessages.clear();
		}

		for (CollectedMessageConsumer drain : consumers) {
			drain.onDrain(immutable);
		}
	}
}
