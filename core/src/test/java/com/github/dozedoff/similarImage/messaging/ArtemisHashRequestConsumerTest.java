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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.imageio.IIOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.commonj.hash.ImagePHash;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;

@RunWith(MockitoJUnitRunner.class)
public class ArtemisHashRequestConsumerTest extends MessagingBaseTest {
	private static final String TEST_ADDRESS_REQUEST = "test_request";
	private static final String TEST_ADDRESS_RESULT = "test_result";
	private static final String TEST_PATH = "foo";
	private static final long TEST_HASH = 42L;

	@Mock
	private ImagePHash hasher;

	private ArtemisHashRequestConsumer cut;

	@Before
	public void setUp() throws Exception {
		when(hasher.getLongHash(any(InputStream.class))).thenReturn(TEST_HASH);
		message = new MockMessageBuilder().configureHashRequestMessage().build();
		
		cut = new ArtemisHashRequestConsumer(session, hasher, TEST_ADDRESS_REQUEST, TEST_ADDRESS_RESULT);
	}

	@Test
	public void testMessageSent() throws Exception {
		cut.onMessage(message);

		verify(producer).send(sessionMessage);
	}

	@Test
	public void testMessagePathProperty() throws Exception {
		cut.onMessage(message);

		verify(sessionMessage).putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, TEST_PATH);
	}

	@Test
	public void testMessageHashProperty() throws Exception {
		cut.onMessage(message);

		verify(sessionMessage).putLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY, TEST_HASH);
	}

	@Test
	public void testMessageCorruptImageTask() throws Exception {
		message = new MockMessageBuilder().configureCorruptImageMessage().build();
		when(hasher.getLongHash(any(InputStream.class))).thenThrow(new IIOException(""));

		cut.onMessage(message);

		verify(sessionMessage).putStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY, ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT);
	}

	@Test
	public void testNoHashSetOnCorruptImageMessage() throws Exception {
		when(hasher.getLongHash(any(InputStream.class))).thenThrow(new IIOException(""));

		cut.onMessage(message);

		verify(sessionMessage, never()).putLongProperty(eq(ArtemisHashProducer.MESSAGE_HASH_PROPERTY), any(Long.class));
	}

	@Test
	public void testMessageCorruptImageSent() throws Exception {
		message = new MockMessageBuilder().configureCorruptImageMessage().build();
		when(hasher.getLongHash(any(InputStream.class))).thenThrow(new IIOException(""));

		cut.onMessage(message);

		verify(producer).send(sessionMessage);
	}
}
