package com.github.dozedoff.similarImage.messaging;

import java.nio.file.Path;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.ArtemisQueue.QueueAddress;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

public class TaskMessageHandler implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageHandler.class);

	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;
	private final ClientProducer producer;
	private final MessageFactory messageFactory;

	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository, ClientSession session)
			throws ActiveMQException {
		this(pendingRepository, imageRepository, session, QueueAddress.EA_UPDATE.toString());
	}

	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository, ClientSession session,
			String eaUpdateAddress) throws ActiveMQException {
		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
		this.producer = session.createProducer(eaUpdateAddress);
		this.messageFactory = new MessageFactory(session);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMessage(ClientMessage msg) {
		try {
			if (isTaskType(msg, TaskType.result)) {
				long hash = msg.getLongProperty(MessageProperty.hashResult.toString());
				int trackingId = msg.getIntProperty(MessageProperty.id.toString());
				LOGGER.trace("Received result message with id {} and hash {}", trackingId, hash);

				PendingHashImage pending = pendingRepository.getById(trackingId);
				storeHash(pending.getPathAsPath(), hash);
				pendingRepository.removeById(trackingId);
				ClientMessage eaUpdate = messageFactory.eaUpdate(pending.getPathAsPath(), hash);

				try {
					producer.send(eaUpdate);
					LOGGER.trace("Sent EA update for {}", pending.getPath());
				} catch (ActiveMQException e) {
					LOGGER.warn("Failed to send ea update message for {}: {}", pending.getPath(), e.toString());
				}
			} else {
				LOGGER.error("Unhandled message: {}", msg);
			}
		} catch (RepositoryException e) {
			LOGGER.warn("Failed to store result message: {}", e.toString());
		}
	}

	private void storeHash(Path path, long hash) throws RepositoryException {
		LOGGER.debug("Creating record for {} with hash {}", path, hash);
		imageRepository.store(new ImageRecord(path.toString(), hash));
	}

	private boolean isTaskType(ClientMessage message, TaskType task) {
		return task.toString().equals(message.getStringProperty(MessageProperty.task.toString()));
	}
}
