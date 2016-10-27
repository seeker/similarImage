package com.github.dozedoff.similarImage.messaging;

import java.nio.file.Path;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

public class TaskMessageHandler implements MessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskMessageHandler.class);

	private final PendingHashImageRepository pendingRepository;
	private final ImageRepository imageRepository;

	public TaskMessageHandler(PendingHashImageRepository pendingRepository, ImageRepository imageRepository) {
		this.pendingRepository = pendingRepository;
		this.imageRepository = imageRepository;
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
				LOGGER.debug("Received result message with id {} and hash {}", trackingId, hash);

				PendingHashImage pending = pendingRepository.getById(trackingId);
				storeHash(pending.getPathAsPath(), hash);
				pendingRepository.removeById(trackingId);
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
