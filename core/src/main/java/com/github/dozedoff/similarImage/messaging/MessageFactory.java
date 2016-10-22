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

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;

/**
 * Used to create pre-configured messages.
 * 
 * @author Nicholas Wright
 *
 */
public class MessageFactory {
	public static final String TRACKING_PROPERTY_NAME = "id";
	public static final String HASH_PROPERTY_NAME = "hashResult";

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

	/**
	 * Create a new message for a hashing request.
	 * 
	 * @param resizedImage
	 *            resized image to be hashed
	 * @param trackingId
	 *            used to track the original path of the image
	 * @return configured message
	 */
	public ClientMessage hashRequestMessage(byte[] resizedImage, int trackingId) {
		ClientMessage message = session.createMessage(true);

		message.putIntProperty(TRACKING_PROPERTY_NAME, trackingId);
		message.getBodyBuffer().writeBytes(resizedImage);

		return message;
	}

	/**
	 * Create a new message for the hashing result
	 * 
	 * @param hash
	 *            that was calculated
	 * @param trackingId
	 *            used to track the original path of the image
	 * @return configured message
	 */
	public ClientMessage resultMessage(long hash, int trackingId) {
		ClientMessage message = session.createMessage(true);

		message.putIntProperty(TRACKING_PROPERTY_NAME, trackingId);
		message.putLongProperty(HASH_PROPERTY_NAME, hash);

		return message;
	}
}
