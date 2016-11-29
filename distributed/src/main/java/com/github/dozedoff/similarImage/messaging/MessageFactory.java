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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;

import com.github.dozedoff.similarImage.db.PendingHashImage;

/**
 * Used to create pre-configured messages.
 * 
 * @author Nicholas Wright
 *
 */
public class MessageFactory {

	/**
	 * Property name in the message
	 */
	public enum MessageProperty {
		repository_query,

		/**
		 * @deprecated Use UUIDs messages from {@link MessageFactory#trackPath(Path, UUID)} instead.
		 */
		@Deprecated
		id, hashResult, task, path
	}

	/**
	 * What kind of query this message represents
	 */
	public enum QueryType {
		pending, TRACK
	};

	/**
	 * What kind of task this message represents
	 */
	public enum TaskType {
		hash, corr, result, eaupdate, track
	};

	/**
	 * @deprecated Use enum {@link MessageProperty}.
	 */
	@Deprecated
	public static final String TRACKING_PROPERTY_NAME = MessageProperty.id.toString();
	/**
	 * @deprecated Use enum {@link MessageProperty}.
	 */
	@Deprecated
	public static final String HASH_PROPERTY_NAME = MessageProperty.hashResult.toString();
	/**
	 * @deprecated Use enum {@link MessageProperty}.
	 */
	@Deprecated
	public static final String QUERY_PROPERTY_NAME = MessageProperty.repository_query.toString();

	/**
	 * @deprecated Use enum {@link QueryType}.
	 */
	@Deprecated
	public static final String QUERY_PROPERTY_VALUE_PENDING = QueryType.pending.toString();
	/**
	 * @deprecated Use enum {@link QueryType}.
	 */
	@Deprecated
	public static final String QUERY_PROPERTY_VALUE_TRACK = QueryType.TRACK.toString();

	private final ClientSession session;

	/**
	 * Create a new factory with the given session.
	 * 
	 * @param session
	 *            to use for creating new messages
	 */
	public MessageFactory(ClientSession session) {
		this.session = session;
	}

	private void setQueryType(ClientMessage message, QueryType type) {
		message.putStringProperty(MessageProperty.repository_query.toString(), type.toString());
	}

	private void setTaskType(ClientMessage message, TaskType task) {
		message.putStringProperty(MessageProperty.task.toString(), task.toString());
	}

	private void setPath(ClientMessage message, Path value) {
		message.putStringProperty(MessageProperty.path.toString(), value.toString());
	}

	/**
	 * Create a new message for a hashing request.
	 * 
	 * @param resizedImage
	 *            resized image to be hashed
	 * @param uuid
	 *            used to track the original path of the image
	 * @return configured message
	 */
	public ClientMessage hashRequestMessage(byte[] resizedImage, UUID uuid) {
		ClientMessage message = session.createMessage(true);

		message.getBodyBuffer().writeLong(uuid.getMostSignificantBits());
		message.getBodyBuffer().writeLong(uuid.getLeastSignificantBits());
		message.getBodyBuffer().writeBytes(resizedImage);

		return message;
	}

	/**
	 * Create a new message for the hashing result.
	 * 
	 * @param hash
	 *            that was calculated
	 * @param most
	 *            most significant bits of the {@link UUID}
	 * @param least
	 *            least significant bits of the {@link UUID}
	 * @return configured message
	 */
	public ClientMessage resultMessage(long hash, long most, long least) {
		ClientMessage message = session.createMessage(true);

		setTaskType(message, TaskType.result);
		ActiveMQBuffer buffer = message.getBodyBuffer();

		buffer.writeLong(most);
		buffer.writeLong(least);
		buffer.writeLong(hash);

		return message;
	}

	/**
	 * Create a new message for a corrupt image.
	 * 
	 * @param path
	 *            of the corrupt image
	 * @return configured message
	 */
	public ClientMessage corruptMessage(Path path) {
		ClientMessage message = session.createMessage(true);

		message.putStringProperty(MessageProperty.path.toString(), path.toString());
		message.putStringProperty(MessageProperty.task.toString(), TaskType.corr.toString());

		return message;
	}

	/**
	 * Query the repository for all pending images.
	 * 
	 * @return configured message
	 */
	public ClientMessage pendingImageQuery() {
		ClientMessage message = session.createMessage(false);

		message.putStringProperty(QUERY_PROPERTY_NAME, QUERY_PROPERTY_VALUE_PENDING);

		return message;
	}

	/**
	 * Create a response message for a pending images query.
	 * 
	 * @param pendingImages
	 *            a list of pending messages
	 * @return configured message
	 * @throws IOException
	 *             if there was an error writing the object stream
	 */
	public ClientMessage pendingImageResponse(List<PendingHashImage> pendingImages) throws IOException {
		List<String> pendingPaths = new LinkedList<String>();
		// TODO write list size followed by strings. Avoid serialization
		for (PendingHashImage p : pendingImages) {
			pendingPaths.add(p.getPath());
		}

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			ClientMessage message = session.createMessage(false);

			oos.writeObject(pendingPaths);
			message.writeBodyBufferBytes(baos.toByteArray());

			return message;
		}
	}

	/**
	 * Create a query message for a tracking id for the given path.
	 * 
	 * @param path
	 *            to query and track
	 * @return configured message
	 * @deprecated Use UUID tracking messages from {@link MessageFactory#trackPath(Path, UUID)} instead.
	 */
	@Deprecated
	public ClientMessage trackPathQuery(Path path) {
		ClientMessage message = session.createMessage(false);
		setQueryType(message, QueryType.TRACK);

		message.getBodyBuffer().writeString(path.toString());

		return message;
	}

	/**
	 * Create a response message for a tracking id query.
	 * 
	 * @param trackingId
	 *            for the path in the query
	 * @return configured message
	 * @deprecated Use UUID tracking messages from {@link MessageFactory#trackPath(Path, UUID)} instead.
	 */
	@Deprecated
	public ClientMessage trackPathResponse(int trackingId) {
		ClientMessage message = session.createMessage(false);

		message.getBodyBuffer().writeInt(trackingId);

		return message;
	}

	/**
	 * Create a message to track a path with the given {@link UUID}
	 * 
	 * @param path
	 *            to track
	 * @param uuid
	 *            for this path
	 * @return configured message
	 */
	public ClientMessage trackPath(Path path, UUID uuid) {
		ClientMessage message = session.createMessage(false);

		setTaskType(message, TaskType.track);
		setPath(message, path);
		message.getBodyBuffer().writeLong(uuid.getMostSignificantBits());
		message.getBodyBuffer().writeLong(uuid.getLeastSignificantBits());

		return message;
	}

	/**
	 * Create a message for updating extended attributes of files.
	 * 
	 * @param path
	 *            to update
	 * @param hash
	 *            for the file
	 * @return configured message
	 */
	public ClientMessage eaUpdate(Path path, long hash) {
		ClientMessage message = session.createMessage(true);

		setTaskType(message, TaskType.eaupdate);
		setPath(message, path);
		message.getBodyBuffer().writeLong(hash);

		return message;
	}

	/**
	 * Create a message for resizing an image.
	 * 
	 * @param path
	 *            of the image
	 * @param is
	 *            {@link InputStream} to the image file
	 * @return configured message
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public ClientMessage resizeRequest(Path path, InputStream is) throws IOException {
		ClientMessage message = session.createMessage(true);
		copyInputStreamToMessage(is, message);
		setTaskType(message, TaskType.hash);
		setPath(message, path);

		return message;
	}

	private void copyInputStreamToMessage(InputStream is, ClientMessage message) throws IOException {
		ActiveMQBuffer buffer = message.getBodyBuffer();
		
		int value;
		do {
			value = is.read();
			if (value != -1) {
				buffer.writeByte((byte) value);
			}
		} while (value != -1);
	}
}
