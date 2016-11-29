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

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.messaging.MessageFactory.MessageProperty;
import com.github.dozedoff.similarImage.messaging.MessageFactory.QueryType;
import com.github.dozedoff.similarImage.messaging.MessageFactory.TaskType;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.class)
public class MessageFactoryTest extends MessagingBaseTest {
	private static final int EXPECTED_MESSAGE_SIZE = 54;
	private static final long HASH = 12L;
	private static final byte[] IMAGE_DATA = { 0, 1, 2, 3, 4 };
	private static final Path PATH = Paths.get("foo");
	private static final UUID UUID = new UUID(99, 100);

	@Mock
	private InputStream is;

	private MessageFactory cut;

	@Before
	public void setUp() throws Exception {
		when(session.createMessage(true)).thenReturn(new ClientMessageImpl(ClientMessageImpl.DEFAULT_TYPE, true, 0, 0, (byte) 0, 0));
		when(session.createMessage(false)).thenReturn(new ClientMessageImpl(ClientMessageImpl.DEFAULT_TYPE, false, 0, 0, (byte) 0, 0));
		
		when(is.read()).thenReturn(-1);

		cut = new MessageFactory(session);
	}

	@Test
	public void testHashRequestMessageTrackingId() throws Exception {
		ClientMessage result = cut.hashRequestMessage(IMAGE_DATA, UUID);

		UUID id = new UUID(result.getBodyBuffer().readLong(), result.getBodyBuffer().readLong());
		assertThat(id, is(UUID));
	}

	@Test
	public void testHashRequestMessageImageData() throws Exception {
		ClientMessage result = cut.hashRequestMessage(IMAGE_DATA, UUID);
		result.getBodyBuffer().readLong();
		result.getBodyBuffer().readLong();

		byte[] data = new byte[IMAGE_DATA.length];
		result.getBodyBuffer().readBytes(data);

		assertArrayEquals(data, IMAGE_DATA);
	}

	@Test
	public void testHashRequestMessageImageSize() throws Exception {
		ClientMessage result = cut.hashRequestMessage(IMAGE_DATA, UUID);
		result.getBodyBuffer().readLong();
		result.getBodyBuffer().readLong();

		byte[] data = new byte[result.getBodySize()];
		result.getBodyBuffer().readBytes(data);

		assertArrayEquals(data, IMAGE_DATA);
	}

	@Test
	public void testResultMessageTask() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, UUID.getMostSignificantBits(), UUID.getLeastSignificantBits());

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.result.toString()));
	}

	@Test
	public void testResultMessageUuid() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, UUID.getMostSignificantBits(), UUID.getLeastSignificantBits());

		UUID id = new UUID(result.getBodyBuffer().readLong(), result.getBodyBuffer().readLong());

		assertThat(id, is(UUID));
	}

	@Test
	public void testResultMessageHash() throws Exception {
		ClientMessage result = cut.resultMessage(HASH, UUID.getMostSignificantBits(), UUID.getLeastSignificantBits());

		result.getBodyBuffer().readLong();
		result.getBodyBuffer().readLong();
		long hash = result.getBodyBuffer().readLong();

		assertThat(hash, is(HASH));
	}

	@Test
	public void testPendingImageQuery() throws Exception {
		ClientMessage result = cut.pendingImageQuery();

		assertThat(result.getStringProperty(MessageFactory.QUERY_PROPERTY_NAME), is(MessageFactory.QUERY_PROPERTY_VALUE_PENDING));
	}

	@Test
	public void testPendingImageResponse() throws Exception {
		ClientMessage result = cut.pendingImageResponse(Arrays.asList(new PendingHashImage(PATH, UUID)));

		assertThat(result.getBodySize(), is(EXPECTED_MESSAGE_SIZE));
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

	@Test
	public void testEaUpdatePath() throws Exception {
		ClientMessage result = cut.eaUpdate(PATH, HASH);

		assertThat(result.getStringProperty(MessageProperty.path.toString()), is(PATH.toString()));
	}

	@Test
	public void testEaUpdateHash() throws Exception {
		ClientMessage result = cut.eaUpdate(PATH, HASH);

		assertThat(result.getBodyBuffer().readLong(), is(HASH));
	}

	@Test
	public void testEaUpdateTask() throws Exception {
		ClientMessage result = cut.eaUpdate(PATH, HASH);

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.eaupdate.toString()));
	}

	@Test
	public void testResizeRequestPath() throws Exception {
		ClientMessage result = cut.resizeRequest(PATH, is);

		assertThat(result.getStringProperty(MessageProperty.path.toString()), is(PATH.toString()));
	}

	@Test
	public void testResizeRequestTask() throws Exception {
		ClientMessage result = cut.resizeRequest(PATH, is);

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.hash.toString()));
	}

	@Test
	public void testTrackPathPathProperty() throws Exception {
		ClientMessage result = cut.trackPath(PATH, UUID);

		assertThat(result.getStringProperty(MessageProperty.path.toString()), is(PATH.toString()));
	}

	@Test
	public void testTrackPathTaskProperty() throws Exception {
		ClientMessage result = cut.trackPath(PATH, UUID);

		assertThat(result.getStringProperty(MessageProperty.task.toString()), is(TaskType.track.toString()));
	}

	@Test
	public void testTrackPathMessageBody() throws Exception {
		ClientMessage result = cut.trackPath(PATH, UUID);

		long most = result.getBodyBuffer().readLong();
		long least = result.getBodyBuffer().readLong();

		assertThat(new UUID(most, least), is(UUID));
	}

}
