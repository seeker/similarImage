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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;

public class MockMessageBuilderTest {
	private static final String STRING_PROPERTY_NAME = "a string";
	private static final String LONG_PROPERTY_NAME = "a long number";

	private static final String STRING_VALUE_ONE = "foo";
	private static final String STRING_VALUE_TWO = "two";

	private static final long LONG_VALUE = 42L;

	private MockMessageBuilder cut;

	@Before
	public void setUp() throws Exception {
		cut = new MockMessageBuilder();
	}

	@Test
	public void testSetStringProperty() {
		ClientMessage message = cut.addProperty(STRING_PROPERTY_NAME, STRING_VALUE_ONE).build();
		
		assertThat(message.getStringProperty(STRING_PROPERTY_NAME), is(STRING_VALUE_ONE));
	}

	@Test
	public void testStringContainsProperty() {
		ClientMessage message = cut.addProperty(STRING_PROPERTY_NAME, STRING_VALUE_ONE).build();

		assertThat(message.containsProperty(STRING_PROPERTY_NAME), is(true));
	}

	@Test
	public void testSetLongProperty() {
		ClientMessage message = cut.addProperty(LONG_PROPERTY_NAME, LONG_VALUE).build();

		assertThat(message.getLongProperty(LONG_PROPERTY_NAME), is(LONG_VALUE));
	}

	@Test
	public void testContainsLongProperty() {
		ClientMessage message = cut.addProperty(LONG_PROPERTY_NAME, LONG_VALUE).build();

		assertThat(message.containsProperty(LONG_PROPERTY_NAME), is(true));
	}

	@Test
	public void testSetStringPropertyOverwrite() {
		ClientMessage message = cut.addProperty(STRING_PROPERTY_NAME, STRING_VALUE_ONE).addProperty(STRING_PROPERTY_NAME, STRING_VALUE_TWO)
				.build();

		assertThat(message.getStringProperty(STRING_PROPERTY_NAME), is(STRING_VALUE_TWO));
	}

	@Test
	public void testSetStringPropertyOverwriteMultipleCalls() {
		ClientMessage message = cut.addProperty(STRING_PROPERTY_NAME, STRING_VALUE_ONE).addProperty(STRING_PROPERTY_NAME, STRING_VALUE_TWO)
				.build();

		assertThat(message.getStringProperty(STRING_PROPERTY_NAME), is(STRING_VALUE_TWO));
		assertThat(message.getStringProperty(STRING_PROPERTY_NAME), is(STRING_VALUE_TWO));
	}

	@Test
	public void testAddBodyBuffer() {
		ActiveMQBuffer buffer = Mockito.mock(ActiveMQBuffer.class);
		ClientMessage message = cut.addBodyBuffer(buffer).build();

		assertThat(message.getBodyBuffer(), is(notNullValue()));
	}

	@Test
	public void testConfigureHashResultMessageHashProperty() {
		ClientMessage message = cut.configureHashResultMessage().build();

		assertThat(message.getLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY), is(LONG_VALUE));
	}

	@Test
	public void testConfigureHashResultMessagePathProperty() {
		ClientMessage message = cut.configureHashResultMessage().build();

		assertThat(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY), is(STRING_VALUE_ONE));
	}

	@Test
	public void testConfigureCorruptImageMessagePathProperty() {
		ClientMessage message = cut.configureCorruptImageMessage().build();

		assertThat(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY), is(STRING_VALUE_ONE));
	}

	@Test
	public void testConfigureCorruptImageMessageTaskProperty() {
		ClientMessage message = cut.configureCorruptImageMessage().build();

		assertThat(message.getStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY),
				is(ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT));
	}

	@Test
	public void testConfigureHashRequestMessagePathProperty() {
		ClientMessage message = cut.configureHashRequestMessage().build();

		assertThat(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY), is(STRING_VALUE_ONE));
	}

	@Test
	public void testConfigureHashRequestMessageImageDataProperty() {
		ClientMessage message = cut.configureHashRequestMessage().build();

		assertThat(message.getBodyBuffer(), is(notNullValue()));
	}

	@Test
	public void testConfigureResizeMessagePathProperty() {
		ClientMessage message = cut.configureResizeMessage().build();

		assertThat(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY), is(STRING_VALUE_ONE));
	}

	@Test
	public void testConfigureResizeMessageImageData() {
		ClientMessage message = cut.configureResizeMessage().build();

		assertThat(message.getBodyBuffer(), is(notNullValue()));
	}
}
