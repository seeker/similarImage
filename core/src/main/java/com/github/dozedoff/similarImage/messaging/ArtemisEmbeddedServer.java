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

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

/**
 * Setup an embedded Artemis server.
 * 
 * @author Nicholas Wright
 *
 */

public class ArtemisEmbeddedServer {
	private final EmbeddedActiveMQ server;

	/**
	 * Create a new embedded artemis server that listens to network and in VM connections.
	 */
	public ArtemisEmbeddedServer() {
		Configuration config = new ConfigurationImpl();
		Set<TransportConfiguration> transports = new HashSet<TransportConfiguration>();

		transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
		transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

		config.setAcceptorConfigurations(transports);
		config.setSecurityEnabled(false);

		server = new EmbeddedActiveMQ();
		server.setConfiguration(config);
	}

	/**
	 * Start the artemis server.
	 * 
	 * @throws Exception
	 *             if things go pear shaped
	 */
	public void start() throws Exception {
		server.start();
	}

	/**
	 * Stop the artemis server.
	 * 
	 * @throws Exception
	 *             if things go pear shaped
	 */
	public void stop() throws Exception {
		server.stop();
	}
}
