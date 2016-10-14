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

import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;

public class ArtemisSession {
	private final ClientSession session;

	public static final String ADDRESS_HASH_QUEUE = "hashQ";
	public static final String ADDRESS_RESULT_QUEUE = "resultQ";

	/**
	 * Create and configure a Artemis session
	 * 
	 * @param serverLocator
	 *            for finding the servers to connect to
	 * @throws Exception
	 *             if the setup fails
	 */
	public ArtemisSession(ServerLocator serverLocator) throws Exception {
		ClientSessionFactory factory = serverLocator.createSessionFactory();
		session = factory.createSession();
		session.start();

		session.createQueue(ADDRESS_HASH_QUEUE, "hash", false);
		session.createQueue(ADDRESS_RESULT_QUEUE, "result", false);
	}

	/**
	 * Get the configured session
	 * 
	 * @return artemis session
	 */
	public ClientSession getSession() {
		return session;
	}
}
