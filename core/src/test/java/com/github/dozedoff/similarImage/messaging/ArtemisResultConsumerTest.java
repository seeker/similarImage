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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;

@RunWith(MockitoJUnitRunner.class)
public class ArtemisResultConsumerTest extends MessagingBaseTest {
	private static final String TEST_PATH = "foo";
	private static final long TEST_HASH = 42L;

	@Mock
	private ExtendedAttributeQuery eaQuery;

	@Mock
	private HashAttribute hashAttribute;

	@Mock
	private ImageRepository imageRepository;

	private ArtemisResultConsumer cut;

	@Before
	public void setUp() throws Exception {
		when(message.getStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY)).thenReturn(TEST_PATH);
		when(message.getLongProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY)).thenReturn(TEST_HASH);
		
		cut = new ArtemisResultConsumer(session, imageRepository, eaQuery, hashAttribute);
	}

	@Test
	public void testStoreHash() throws Exception {
		when(message.containsProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY)).thenReturn(true);

		cut.onMessage(message);

		verify(imageRepository).store(any(ImageRecord.class));
	}

	@Test
	public void testStoreExtendedAttribute() throws Exception {
		when(message.containsProperty(ArtemisHashProducer.MESSAGE_HASH_PROPERTY)).thenReturn(true);
		when(eaQuery.isEaSupported(any(Path.class))).thenReturn(true);

		cut.onMessage(message);

		verify(hashAttribute).writeHash(Paths.get(TEST_PATH), TEST_HASH);
	}

	@Test
	public void testCorruptImageMessage() throws Exception {
		when(message.getStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY))
				.thenReturn(ArtemisHashProducer.MESSAGE_TASK_VALUE_CORRUPT);
		when(eaQuery.isEaSupported(any(Path.class))).thenReturn(true);

		cut.onMessage(message);

		verify(hashAttribute).markCorrupted(Paths.get(TEST_PATH));
	}
}
