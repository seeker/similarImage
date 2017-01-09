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
package com.github.dozedoff.similarImage.module;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;

import com.github.dozedoff.similarImage.messaging.ArtemisSession;

import dagger.Module;
import dagger.Provides;

@Module
public class ArtemisModule {
	private static final int LARGE_MESSAGE_SIZE_THRESHOLD = 1024 * 1024 * 100;

	private Path serverWorkingDirectory;

	@Inject
	public ArtemisModule() {
		serverWorkingDirectory = Paths.get("");
	}

	public ArtemisModule(Path serverWorkingDirectory) {
		this.serverWorkingDirectory = serverWorkingDirectory;
	}

	@Provides
	public FileConfiguration provideServerConfiguration() {
		try {
			FileConfiguration config = new FileConfiguration();
			FileDeploymentManager fdm = new FileDeploymentManager("broker.xml");
			fdm.addDeployable(config);
			fdm.readConfiguration();
			config.setBrokerInstance(serverWorkingDirectory.toFile());

			return config;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create server configuration");
		}
	}

	private RuntimeException runtimeException(ActiveMQException e) {
		return new RuntimeException("Failed to create session", e);
	}

	@Provides
	@Named("normal")
	public ClientSession provideNormalSession(ArtemisSession artemisSession) {
		try {
			return artemisSession.getSession();
		} catch (ActiveMQException e) {
			throw runtimeException(e);
		}
	}

	@Provides
	@Named("transacted")
	public ClientSession provideTransactedSession(ArtemisSession artemisSession) {
		try {
			return artemisSession.getTransactedSession();
		} catch (ActiveMQException e) {
			throw runtimeException(e);
		}
	}

	@Provides
	public static ClientSessionFactory provideSessionFactory(ServerLocator locator) {
		try {
			return locator.createSessionFactory();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create session factory", e);
		}
	}

	@Provides
	public static ServerLocator provideInVmLocator() {
		return ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()))
				.setCacheLargeMessagesClient(false).setMinLargeMessageSize(LARGE_MESSAGE_SIZE_THRESHOLD)
				.setBlockOnNonDurableSend(false).setBlockOnDurableSend(false).setPreAcknowledge(true);
	}
}
