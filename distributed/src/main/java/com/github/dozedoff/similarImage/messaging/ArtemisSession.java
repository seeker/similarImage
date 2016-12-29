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

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtemisSession implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisSession.class);
	private final ClientSessionFactory factory;

	/**
	 * Create a session factory
	 * 
	 * @param serverLocator
	 *            for finding the servers to connect to
	 * @throws Exception
	 *             if the setup fails
	 */
	public ArtemisSession(ServerLocator serverLocator) throws Exception {
		factory = serverLocator.createSessionFactory();
	}

	private ClientSession createAndConfigureSession() throws ActiveMQException {
		ClientSession session = factory.createSession();
		session.start();
		return session;
	}

	/**
	 * Get a new configured session instance
	 * 
	 * @return artemis session
	 * @throws ActiveMQException
	 *             if the session setup fails
	 */
	public ClientSession getSession() throws ActiveMQException {
		return createAndConfigureSession();
	}

	/**
	 * Get a new configured and running transacted session.
	 * 
	 * @return artemis session
	 * @throws ActiveMQException
	 *             if the session setup fails
	 */
	public ClientSession getTransactedSession() throws ActiveMQException {
		ClientSession session = factory.createTransactedSession();
		session.start();
		return session;
	}

	/**
	 * Closes the session factory and all associated sessions.
	 */
	@Override
	public void close() {
		LOGGER.info("Closing {}", this.getClass().getSimpleName());
		factory.close();
	}
}
