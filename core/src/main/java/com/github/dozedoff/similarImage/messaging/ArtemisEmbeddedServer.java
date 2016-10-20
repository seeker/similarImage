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

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Setup an embedded Artemis server.
 * 
 * @author Nicholas Wright
 *
 */

public class ArtemisEmbeddedServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisEmbeddedServer.class);
	private final EmbeddedActiveMQ server;
	

	private static final int MAX_SIZE = 1024 * 1024;

	/**
	 * Create a new embedded artemis server that listens to network and in VM connections. he data directory will be created in the current
	 * working directory.
	 * 
	 * @throws UnknownHostException
	 *             if the local address cannot be resolved
	 */
	public ArtemisEmbeddedServer() throws UnknownHostException {
		this(Paths.get(""));
	}


	/**
	 * Create a new embedded artemis server that listens to network and in VM connections. The data directory will be created in the given
	 * path.
	 * 
	 * @param workingDirectory
	 *            base directory where the data directory will be created
	 * 
	 * @throws UnknownHostException
	 *             if the local address cannot be resolved
	 */
	public ArtemisEmbeddedServer(Path workingDirectory) throws UnknownHostException {

		Set<TransportConfiguration> transports = new HashSet<TransportConfiguration>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(TransportConstants.HOST_PROP_NAME, Inet4Address.getLocalHost().getHostAddress());
		params.put(TransportConstants.PORT_PROP_NAME, TransportConstants.DEFAULT_PORT);

		transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName(), params));
		transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

		LOGGER.info("Listening on {}:{}", params.get(TransportConstants.HOST_PROP_NAME),
				params.get(TransportConstants.PORT_PROP_NAME));

		AddressSettings as = new AddressSettings();
		as.setMaxSizeBytes(MAX_SIZE);
		as.setAddressFullMessagePolicy(AddressFullMessagePolicy.BLOCK);


		Configuration config = new ConfigurationImpl();
		config.addAddressesSetting(ArtemisQueue.QueueAddress.HASH_REQUEST.toString(), as);
		config.setAcceptorConfigurations(transports);
		config.setSecurityEnabled(false);
		config.setBrokerInstance(workingDirectory.toFile());

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
