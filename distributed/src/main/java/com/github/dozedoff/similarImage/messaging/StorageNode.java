package com.github.dozedoff.similarImage.messaging;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

/**
 * Reads files and creates resize requests. Listens to extended attribute update messages.
 */
public class StorageNode implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageNode.class);

	private final ExtendedAttributeQuery eaQuery;
	private final HashAttribute hashAttribute;
	private final ClientProducer producer;
	private final ClientConsumer consumer;

	/**
	 * Create a instance for handling updates and generating resize requests.
	 * 
	 * @param session
	 *            for talking to the broker
	 * @param eaQuery
	 *            to check for ea support
	 * @param hashAttribute
	 *            to write extended attributes to files
	 * @param resizeAddress
	 *            where to send resize requests
	 * @param eaUpdateAddress
	 *            where to listen for ea updates
	 * @throws ActiveMQException
	 *             if there is a error with the queue
	 */
	public StorageNode(ClientSession session, ExtendedAttributeQuery eaQuery, HashAttribute hashAttribute, String resizeAddress,
			String eaUpdateAddress) throws ActiveMQException {
		this.eaQuery = eaQuery;
		this.hashAttribute = hashAttribute;
		this.consumer = session.createConsumer(eaUpdateAddress);
		this.producer = session.createProducer(resizeAddress);

		this.consumer.setMessageHandler(this);
	}

	/**
	 * Create a instance for handling updates and generating resize requests. Use default addresses.
	 * 
	 * @param session
	 *            for talking to the broker
	 * @param eaQuery
	 *            to check for ea support
	 * @param hashAttribute
	 *            to write extended attributes to files
	 * @throws ActiveMQException
	 *             if there is a error with the queue
	 */
	public StorageNode(ClientSession session, ExtendedAttributeQuery eaQuery, HashAttribute hashAttribute) throws ActiveMQException {
		this(session, eaQuery, hashAttribute, QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.EA_UPDATE.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage message) {
		if (isEaUpdate(message)) {
			Path path = getPath(message);
			long hash = message.getBodyBuffer().readLong();
			hashAttribute.writeHash(path, hash);
		} else if (isCorrupt(message)) {
			Path path = getPath(message);
			try {
				hashAttribute.markCorrupted(path);
			} catch (IOException e) {
				LOGGER.warn("Failed to mark {} as corrupt: {}", path, e.toString());
			}
		} else {
			LOGGER.warn("Unhandled message: {}", message);
		}
	}

	private Path getPath(ClientMessage message) {
		return Paths.get(message.getStringProperty(MessageProperty.path.toString()));
	}

	private boolean isEaUpdate(ClientMessage message) {
		return TaskType.eaupdate.toString().equals(message.getStringProperty(MessageProperty.task.toString()));
	}

	private boolean isCorrupt(ClientMessage message) {
		return TaskType.corr.toString().equals(message.getStringProperty(MessageProperty.task.toString()));
	}

	public void processFile(Path path) {
		throw new RuntimeException("Not implemented");
	}
}
