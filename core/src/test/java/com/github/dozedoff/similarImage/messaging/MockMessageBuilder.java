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

import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.mockito.Mockito;

import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;

public class MockMessageBuilder {
	private final ClientMessage message;

	private static final String DEFAULT_PATH = "foo";
	private static final long DEFAULT_HASH = 42L;

	/**
	 * Prepare a mock message builder
	 */
	public MockMessageBuilder() {
		message = Mockito.mock(ClientMessage.class);
	}

	/**
	 * Add a property to the message.
	 * 
	 * @apiNote Multiple calls with the same name will overwrite the previous value.
	 * 
	 * @param name
	 *            of the property to set
	 * @param value
	 *            to set for the property
	 * @return {@link MockMessageBuilder} instance to chain building
	 */
	public MockMessageBuilder addProperty(String name, String value) {
		when(message.getStringProperty(name)).thenReturn(value);
		setContainsProperty(name);
		return this;
	}

	/**
	 * Add a property to the message.
	 * 
	 * @apiNote Multiple calls with the same name will overwrite the previous value.
	 * 
	 * @param name
	 *            of the property to set
	 * @param value
	 *            to set for the property
	 * @return {@link MockMessageBuilder} instance to chain building
	 */
	public MockMessageBuilder addProperty(String name, long value) {
		when(message.getLongProperty(name)).thenReturn(value);
		setContainsProperty(name);
		return this;
	}

	/**
	 * Add a buffer to the message.
	 * 
	 * @apiNote Multiple calls will overwrite the previous value.
	 * 
	 * @param buffer
	 *            to set
	 * 
	 * @return {@link MockMessageBuilder} instance to chain building
	 */
	public MockMessageBuilder addBodyBuffer(ActiveMQBuffer buffer) {
		when(message.getBodyBuffer()).thenReturn(buffer);
		return this;
	}

	/**
	 * Finish building and return a configured mock {@link ClientMessage}.
	 * 
	 * @return the {@link ClientMessage} you just configured.
	 */
	public ClientMessage build() {
		return message;
	}

	/**
	 * Configure a message that is sent when a image was successfully hashed.
	 * 
	 * @return {@link MockMessageBuilder} for further configuration
	 */
	public MockMessageBuilder configureHashResultMessage() {
		addProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY, DEFAULT_HASH);
		addProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, DEFAULT_PATH);
		return this;
	}

	/**
	 * Configure a message that is sent when a image is corrupted.
	 * 
	 * @return {@link MockMessageBuilder} for further configuration
	 */
	public MockMessageBuilder configureCorruptImageMessage() {
		addProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY, ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT);
		addProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, DEFAULT_PATH);
		return this;
	}

	/**
	 * Configure a message that is sent to request a hash of an image.
	 * 
	 * @return {@link MockMessageBuilder} for further configuration
	 */
	public MockMessageBuilder configureHashRequestMessage() {
		addBodyBuffer(Mockito.mock(ActiveMQBuffer.class));
		addProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, DEFAULT_PATH);
		return this;
	}

	private void setContainsProperty(String property) {
		when(message.containsProperty(property)).thenReturn(true);
	}
}
