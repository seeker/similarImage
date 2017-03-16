package com.github.dozedoff.similarImage.messaging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

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
import com.github.dozedoff.similarImage.util.MessagingUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Reads files and creates resize requests. Listens to extended attribute update messages.
 */
public class StorageNode implements MessageHandler, Node {
	private static final Logger LOGGER = LoggerFactory.getLogger(StorageNode.class);

	private final ExtendedAttributeQuery eaQuery;
	private final HashAttribute hashAttribute;
	private final ClientProducer producer;
	private final ClientConsumer consumer;
	private final MessageFactory messageFactory;
	private final Cache<Path, Integer> sentRequests;

	/**
	 * Create a instance for handling updates and generating resize requests.
	 * 
	 * @param session
	 *            for talking to the broker
	 * @param eaQuery
	 *            to check for ea support
	 * @param hashAttribute
	 *            to write extended attributes to files
	 * @param pendingPaths
	 *            list of paths that are pending, will be added to the cache
	 * @param resizeAddress
	 *            where to send resize requests
	 * @param eaUpdateAddress
	 *            where to listen for ea updates
	 */
	public StorageNode(ClientSession session, ExtendedAttributeQuery eaQuery, HashAttribute hashAttribute, List<Path> pendingPaths,
			String resizeAddress, String eaUpdateAddress) {
		try {
		this.eaQuery = eaQuery;
		this.hashAttribute = hashAttribute;
		this.consumer = session.createConsumer(eaUpdateAddress);
		this.producer = session.createProducer(resizeAddress);
		this.messageFactory = new MessageFactory(session);

		sentRequests = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();

		this.consumer.setMessageHandler(this);
			loadSentRequestCache(pendingPaths);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create " + StorageNode.class.getSimpleName(), e);
		}
	}

	private void loadSentRequestCache(List<Path> pendingPaths) {
		for (Path path : pendingPaths) {
			sentRequests.put(path, 0);
		}
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
	 * @param pendingPaths
	 *            list of paths that are pending, will be added to the cache
	 */
	@Inject
	public StorageNode(@Named("normal") ClientSession session, ExtendedAttributeQuery eaQuery,
			HashAttribute hashAttribute, @Named("pending") List<Path> pendingPaths) {
		this(session, eaQuery, hashAttribute, pendingPaths, QueueAddress.RESIZE_REQUEST.toString(), QueueAddress.EA_UPDATE.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage message) {
		LOGGER.trace("Got message {}", message);
		if (isEaUpdate(message)) {
			Path path = getPath(message);
			long hash = message.getBodyBuffer().readLong();
			hashAttribute.writeHash(path, hash);
			LOGGER.trace("Updated EA for {} with hash {}", path, hash);
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

	/**
	 * Send a request to process the file. This will generate and send a new resize request. The class keeps track of the files sent within
	 * 
	 * @param path
	 *            to process
	 * @return true if the request was sent
	 */
	public boolean processFile(Path path) {
		LOGGER.trace("Processing {}", path);
		if (isAlreadySent(path)) {
			LOGGER.trace("File {} has already been sent, ignoring...", path);
			return true;
		}

		try (InputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
			ClientMessage request = messageFactory.resizeRequest(path, bis);
			producer.send(request);
			sentRequests.put(path, 0);
			LOGGER.trace("Sent resize request for {}", path);
			return true;
		} catch (IOException e) {
			LOGGER.warn("Failed to access file {}: {}", path, e.toString());
		} catch (ActiveMQException e) {
			LOGGER.warn("Failed to send resize request for {}: {}", path, e.toString());
		}

		return false;
	}

	private boolean isAlreadySent(Path path) {
		return sentRequests.getIfPresent(path) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		MessagingUtil.silentClose(consumer);
		MessagingUtil.silentClose(producer);
	}

	/**
	 * Returns the class name.
	 * 
	 * @return the name of this class
	 */
	@Override
	public String toString() {
		return StorageNode.class.getSimpleName();
	}
}
