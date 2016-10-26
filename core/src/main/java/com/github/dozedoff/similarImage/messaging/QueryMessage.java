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
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientRequestor;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;

/**
 * For request-response messaging
 * 
 * @author Nicholas Wright
 *
 */
public class QueryMessage {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryMessage.class);

	private static final int QUERY_TIMEOUT_MILLIS = 5000;
	private static final String QUERY_TIMEOUT_ERROR_MESSAGE = "Did not get a query response within the timelimit";

	private final ClientSession session;
	private final MessageFactory messageFactory;
	private final ClientRequestor repositoryQuery;

	
	/**
	 * Create a new {@link QueryMessage} instance using the given session.
	 * 
	 * @param session
	 *            to use for messaging
	 * @param queryAddress
	 *            address where query requests are received and responses sent
	 * @throws Exception
	 *             if there was an error setting up the requestors
	 * @deprecated Use constructor with enum.
	 */
	@Deprecated
	public QueryMessage(ClientSession session, String queryAddress) throws Exception {
		this.session = session;
		LOGGER.info("Preparing to send query requests on {} ...", queryAddress);
		repositoryQuery = new ClientRequestor(session, queryAddress);
		messageFactory = new MessageFactory(session);
	}

	/**
	 * Create a new {@link QueryMessage} instance using the given session.
	 * 
	 * @param session
	 *            to use for messaging
	 * @param queryAddress
	 *            address where query requests are received and responses sent
	 * @throws Exception
	 *             if there was an error setting up the requestors
	 */
	public QueryMessage(ClientSession session, QueueAddress queryAddress) throws Exception {
		this.session = session;
		LOGGER.info("Preparing to send query requests on {} ...", queryAddress.toString());
		repositoryQuery = new ClientRequestor(session, queryAddress.toString());
		messageFactory = new MessageFactory(session);
	}

	/**
	 * Get a list of paths that are currently waiting to be hashed.
	 * 
	 * @return list of image paths
	 * @throws Exception
	 *             if there was an error performing the query
	 */
	public List<String> pendingImagePaths() throws Exception {
		LOGGER.debug("Sending pending image query request...");
		ClientMessage queryResponse = repositoryQuery.request(messageFactory.pendingImageQuery(),
				QUERY_TIMEOUT_MILLIS);

		if (queryResponse == null) {
			throw new TimeoutException(QUERY_TIMEOUT_ERROR_MESSAGE);
		}

		ByteBuffer buffer = ByteBuffer.allocate(queryResponse.getBodySize());
		queryResponse.getBodyBuffer().readBytes(buffer);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer.array()));

		@SuppressWarnings("unchecked")
		List<String> pending = (List<String>) ois.readObject();
		LOGGER.debug("Got response for pending images with {} entries", pending.size());
		return pending;
	}

	/**
	 * Request a unique tracking id for the path.
	 * 
	 * @param path
	 *            to track
	 * @return a unique tracking id for the requested path, or -1 if the path is already tracked
	 * @throws Exception
	 *             if there was an error performing the query
	 */
	public int trackPath(Path path) throws Exception {
		LOGGER.trace("Sending path track request for {}", path);

		ClientMessage queryResponse = repositoryQuery.request(messageFactory.trackPathQuery(path), QUERY_TIMEOUT_MILLIS);

		if (queryResponse == null) {
			throw new TimeoutException(QUERY_TIMEOUT_ERROR_MESSAGE);
		}

		return queryResponse.getBodyBuffer().readInt();
	}
}
