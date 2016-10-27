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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.QueryType;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

@RunWith(MockitoJUnitRunner.class)
public class MessageFactoryTest extends MessagingBaseTest {
	private static final int TRACKING_ID = 42;
	private static final long HASH = 12L;
	private static final byte[] IMAGE_DATA = { 0, 1, 2, 3, 4 };
	private static final Path PATH = Paths.get("foo");

	private MessageFactory cut;

	@Before
	public void setUp() throws Exception {
		when(session.createMessage(true)).thenReturn(new ClientMessageImpl(ClientMessageImpl.DEFAULT_TYPE, true, 0, 0, (byte) 0, 0));

		when(session.createMessage(false)).thenReturn(new ClientMessageImpl(ClientMessageImpl.DEFAULT_TYPE, false, 0, 0, (byte) 0, 0));

		cut = new MessageFactory(session);
	}

	@Test
	public void testHashRequestMessageTrackingId() throws Exception {
		ClientMessage result = cut.hashRequestMessage(IMAGE_DATA, TRACKING_ID);

		assertThat(result.getIntProperty(MessageFactory.TRACKING_PROPERTY_NAME), is(TRACKING_ID));
	}

	@Test
	public void testHashRequestMessageImageData() throws Exception {
		ClientMessage result = cut.hashRequestMessage(IMAGE_DATA, TRACKING_ID);

		byte[] data = new byte[IMAGE_DATA.length];
		result.getBodyBuffer().readBytes(data);

		assertArrayEquals(data, IMAGE_DATA);
	}

	@Test
	public void testResultMessageTask() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, TRACKING_ID);

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.result.toString()));
	}

	@Test
	public void testResultMessageTrackingId() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, TRACKING_ID);

		assertThat(result.getIntProperty(MessageFactory.TRACKING_PROPERTY_NAME), is(TRACKING_ID));
	}

	@Test
	public void testResultMessageHash() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, TRACKING_ID);

		assertThat(result.getLongProperty(MessageFactory.HASH_PROPERTY_NAME), is(HASH));
	}

	@Test
	public void testPendingImageQuery() throws Exception {
		ClientMessage result = cut.pendingImageQuery();

		assertThat(result.getStringProperty(MessageFactory.QUERY_PROPERTY_NAME), is(MessageFactory.QUERY_PROPERTY_VALUE_PENDING));
	}

	@Test
	public void testPendingImageResponse() throws Exception {
		ClientMessage result = cut.pendingImageResponse(Arrays.asList(new PendingHashImage(PATH)));

		assertThat(result.getBodySize(), is(54));
	}

	@Test
	public void testTrackPathQueryRequestType() throws Exception {
		ClientMessage result = cut.trackPathQuery(PATH);

		assertThat(result.getStringProperty(MessageProperty.repository_query.toString()), is(QueryType.TRACK.toString()));
	}

	@Test
	public void testTrackPathQueryRequestMessage() throws Exception {
		ClientMessage result = cut.trackPathQuery(PATH);

		assertThat(result.getBodyBuffer().readString(), is(PATH.toString()));
	}

	@Test
	public void testCorruptMessagePath() throws Exception {
		ClientMessage result = cut.corruptMessage(PATH);

		assertThat(result.getStringProperty(MessageProperty.path.toString()), is(PATH.toString()));
	}

	@Test
	public void testCorruptMessageTask() throws Exception {
		ClientMessage result = cut.corruptMessage(PATH);

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.corr.toString()));
	}
}
