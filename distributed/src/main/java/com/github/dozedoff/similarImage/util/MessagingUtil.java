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
package com.github.dozedoff.similarImage.util;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagingUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessagingUtil.class);
	private MessagingUtil() {
	}

	/**
	 * Closes a consumer and logs a warning if an exception is thrown.
	 * 
	 * @param consumer
	 *            to close
	 */
	public static void silentClose(ClientConsumer consumer) {
		try {
			consumer.close();
		} catch (ActiveMQException e) {
			LOGGER.warn("Failed to close consumer: {}", e.toString());
		}
	}

	/**
	 * Closes a producer and logs a warning if an exception is thrown.
	 * 
	 * @param producer
	 *            to close
	 */
	public static void silentClose(ClientProducer producer) {
		try {
			producer.close();
		} catch (ActiveMQException e) {
			LOGGER.warn("Failed to close producer: {}", e.toString());
		}
	}

	/**
	 * Closes a session and logs a warning if an exception is thrown.
	 * 
	 * @param session
	 *            to close
	 */
	public static void silentClose(ClientSession session) {
		try {
			session.close();
		} catch (ActiveMQException e) {
			LOGGER.warn("Failed to close session: {}", e.toString());
		}
	}
}
