package com.github.dozedoff.similarImage.learning;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashSet;

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
import org.junit.BeforeClass;
import org.junit.Test;

public class ArtemisLearning {
	private static EmbeddedActiveMQ server;

	private static ClientProducer producer;
	private static ClientConsumer consumer;
	private static ClientSession session;

	private static final String ADDRESS = "example";
	private static final String QUEUE_NAME = ADDRESS;
	private static final String TEST_MESSAGE = "message";

	@BeforeClass
	public static void setUp() throws Exception {
		// From https://activemq.apache.org/artemis/docs/1.0.0/embedding-activemq.html

		Configuration config = new ConfigurationImpl();
		HashSet<TransportConfiguration> transports = new HashSet<TransportConfiguration>();

		transports.add(new TransportConfiguration(NettyAcceptorFactory.class.getName()));
		transports.add(new TransportConfiguration(InVMAcceptorFactory.class.getName()));

		config.setAcceptorConfigurations(transports);
		config.setSecurityEnabled(false);

		server = new EmbeddedActiveMQ();
		server.setConfiguration(config);

		server.start();

		ServerLocator locator = ActiveMQClient
				.createServerLocatorWithoutHA(new TransportConfiguration(InVMConnectorFactory.class.getName()));

		ClientSessionFactory factory = locator.createSessionFactory();
		session = factory.createSession();

		session.createQueue(ADDRESS, QUEUE_NAME, false);

		producer = session.createProducer(ADDRESS);
		consumer = session.createConsumer(ADDRESS);

		session.start();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.stop();
	}

	@Test
	public void testSendMessage() throws Exception {
		Message msg = session.createMessage(false);
		msg.getBodyBuffer().writeString(TEST_MESSAGE);
		producer.send(msg);

		ClientMessage rMsg = consumer.receive();

		assertThat(rMsg.getBodyBuffer().readString(), is(TEST_MESSAGE));
	}
}
