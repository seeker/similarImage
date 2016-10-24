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
package com.github.dozedoff.similarImage.handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.messaging.MessageFactory;
import com.github.dozedoff.similarImage.messaging.QueryMessage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Creates Messages from the files and sends them to the queue.
 * 
 * @author Nicholas Wright
 *
 */
public class ArtemisHashProducer implements HashHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisHashProducer.class);
	private static final String PLACE_HOLDER = "";
	// TODO move this or use enums?
	public static final String MESSAGE_TASK_PROPERTY = "task";
	public static final String MESSAGE_PATH_PROPERTY = "path";
	/**
	 * @deprecated Use {@link MessageFactory}
	 */
	@Deprecated
	public static final String MESSAGE_HASH_PROPERTY = MessageFactory.HASH_PROPERTY_NAME;
	public static final String MESSAGE_TASK_VALUE_HASH = "hash";
	public static final String MESSAGE_TASK_VALUE_CORRUPT = "corr";

	private final ClientProducer producer;
	private final ClientSession session;

	private Cache<String, String> pendingImages;

	/**
	 * Create a new handler using the given producer.
	 * 
	 * @param session
	 *            used to transfer hash requests
	 * 
	 * @param address
	 *            where hash requests will be sent
	 * @param query
	 *            used request-response queries
	 * @throws Exception
	 *             if pending image query failed
	 */
	public ArtemisHashProducer(ClientSession session, String address, QueryMessage query) throws Exception {
		this.session = session;
		this.producer = session.createProducer(address);

		pendingImages = CacheBuilder.newBuilder().build();
		if (query == null) {
			LOGGER.warn("Query is null, cannot check for duplicates");
		} else {
			List<String> pending = query.pendingImagePaths();
			for (String s : pending) {
				pendingImages.put(s, PLACE_HOLDER);
			}
		}
	}

	/**
	 * Create a new handler using the given producer.
	 * 
	 * @param session
	 *            used to transfer hash requests
	 * 
	 * @param address
	 *            where hash requests will be sent
	 * @throws Exception
	 *             if pending image query failed
	 */
	public ArtemisHashProducer(ClientSession session, String address) throws Exception {
		this(session, address, null);
	}

	// TODO browse hash queue to get ids, then resolve them to paths

	/**
	 * Read the file, create a message and send it.
	 * 
	 * @param file
	 *            to read and send
	 * @return true if the file was read and sent successfully
	 */
	@Override
	public boolean handle(Path file) {
		if (pendingImages.getIfPresent(file.toString()) != null) {
			LOGGER.debug("Image {} is already pending, ignoring", file);
			return true;
		}

		try (InputStream bis = new BufferedInputStream(Files.newInputStream(file))) {
			ClientMessage msg = session.createMessage(false);

			msg.setBodyInputStream(bis);
			msg.putStringProperty(MESSAGE_TASK_PROPERTY, MESSAGE_TASK_VALUE_HASH);
			msg.putStringProperty(MESSAGE_PATH_PROPERTY, file.toString());

			producer.send(msg);
			LOGGER.debug("Sent resize request message for {}", file);
			return true;
		} catch (IOException e) {
			LOGGER.error("Failed to open file {}: {}", file, e.toString());
		} catch (ActiveMQException e) {
			LOGGER.error("Failed to send message: {}", e.toString());
		}

		return false;
	}
}
