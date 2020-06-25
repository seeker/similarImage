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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.github.dozedoff.similarImage.component.DaggerMessagingComponent;
import com.github.dozedoff.similarImage.component.DaggerPersistenceComponent;
import com.github.dozedoff.similarImage.component.MessagingComponent;
import com.github.dozedoff.similarImage.component.PersistenceComponent;
import com.github.dozedoff.similarImage.module.ArtemisModule;

public abstract class MessagingBaseTest {
	protected ClientSession session;
	private static ArtemisEmbeddedServer artemisServer;
	private static MessagingComponent messagingComponent;
	private static final List<String> queues = Arrays.asList("test_request", "test_result");
	
	@BeforeClass
	public static void setupMessagingForClass() throws Exception {
		PersistenceComponent coreComponent = DaggerPersistenceComponent.create();
		messagingComponent = DaggerMessagingComponent.builder().artemisModule(new ArtemisModule(Paths.get("testing"))).persistenceComponent(coreComponent)
				.build();
		artemisServer = messagingComponent.getServer();
		artemisServer.start();
		
		ClientSession setupSession = messagingComponent.getSessionModule().getSession();
		createQueues(setupSession);
	}
	
	@Before
	public void messagingSetup() throws Exception {
		session = messagingComponent.getSessionModule().getSession();
	}
	
	private static void createQueues(ClientSession setupSession) throws ActiveMQException {
		for(String queueName : queues) {
			setupSession.createQueue(queueName, RoutingType.ANYCAST, queueName);
		}
	}
	
	@After
	public void messagingTearDown() throws ActiveMQException {
		session.close();
	}

	@AfterClass
	public static void tearDownMessagingForClass() throws Exception {
		artemisServer.stop();
	}
}
