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
package com.github.dozedoff.similarImage.learning;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.dozedoff.similarImage.util.TestUtil;

public class ArtemisLearning {
	private static EmbeddedActiveMQ server;

	private static ClientProducer producer;
	private static ClientConsumer consumer;
	private static ClientSession session;

	private static ClientProducer transactedProducer;
	private static ClientConsumer transactedConsumer;
	private static ClientSession transactedSession;

	private static final String ADDRESS = "example";
	private static final String QUEUE_NAME = ADDRESS;
	private static final String TRANSACTION_ADDRESS = "transaction";

	private static final int RECEIVE_TIMEOUT = 500;

	private static ClientSessionFactory factory;


	private static Path dataDirectory;
	private double messageTestValue;

	@BeforeClass
	public static void setUpClass() throws Exception {
		// From https://activemq.apache.org/artemis/docs/1.0.0/embedding-activemq.html

		Set<TransportConfiguration> transports = new HashSet<TransportConfiguration>();

		transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
		transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

		dataDirectory = Files.createTempDirectory("Artemis learning");

		Configuration config = new ConfigurationImpl();
		config.setAcceptorConfigurations(transports);
		config.setSecurityEnabled(false);
		config.setBrokerInstance(dataDirectory.toFile());

		server = new EmbeddedActiveMQ();
		server.setConfiguration(config);
		server.start();

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));

		factory = locator.createSessionFactory();
		session = factory.createSession();
		session.start();

		transactedSession = factory.createTransactedSession();
		transactedSession.start();

		session.createQueue(ADDRESS, QUEUE_NAME, false);
		session.createQueue(TRANSACTION_ADDRESS, TRANSACTION_ADDRESS, false);

		producer = session.createProducer(ADDRESS);
		consumer = session.createConsumer(ADDRESS);

		transactedProducer = transactedSession.createProducer(TRANSACTION_ADDRESS);
		transactedConsumer = transactedSession.createConsumer(TRANSACTION_ADDRESS);

		session.start();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		server.stop();
		TestUtil.deleteAllFiles(dataDirectory);
	}

	@Before
	public void setUp() {
		messageTestValue = Math.random();
	}

	private ClientMessage createMessage(ClientSession sessionToUse, double randomValue) {
		ClientMessage message = sessionToUse.createMessage(false);
		message.getBodyBuffer().writeDouble(randomValue);

		return message;
	}

	private void verifyMessage(ClientMessage message, double testValue) {
		assertThat(message, is(notNullValue()));
		assertThat(message.getBodyBuffer().readDouble(), is(testValue));
	}

	@Test
	public void testSendMessage() throws Exception {
		Message msg = createMessage(session, messageTestValue);

		producer.send(msg);

		verifyMessage(consumer.receive(), messageTestValue);
	}

	@Test
	public void testTransactedSession() throws Exception {
		assertThat(transactedSession.isAutoCommitAcks(), is(false));
		assertThat(transactedSession.isAutoCommitSends(), is(false));
	}

	@Test
	public void testTransactedWrite() throws Exception {
		ClientMessage msg = createMessage(transactedSession, messageTestValue);
		transactedProducer.send(ADDRESS, msg);

		assertThat(consumer.receive(RECEIVE_TIMEOUT), is(nullValue()));

		transactedSession.commit();

		verifyMessage(consumer.receive(RECEIVE_TIMEOUT), messageTestValue);
	}

	@Test
	public void testTransactedRead() throws Exception {
		ClientMessage msg = createMessage(session, messageTestValue);
		producer.send(TRANSACTION_ADDRESS, msg);

		ClientMessage rMsg = transactedConsumer.receive(RECEIVE_TIMEOUT);
		verifyMessage(rMsg, messageTestValue);

		rMsg.acknowledge();
		transactedSession.commit();

		assertThat(transactedConsumer.receive(RECEIVE_TIMEOUT), is(nullValue()));
	}

	@Test
	public void testTransactedReadRollback() throws Exception {
		ClientMessage msg = createMessage(session, messageTestValue);
		producer.send(TRANSACTION_ADDRESS, msg);

		verifyMessage(transactedConsumer.receive(RECEIVE_TIMEOUT), messageTestValue);

		transactedSession.rollback();

		ClientMessage rMsg = transactedConsumer.receive(RECEIVE_TIMEOUT);
		verifyMessage(rMsg, messageTestValue);

		rMsg.acknowledge();
		transactedSession.commit();
	}
}
