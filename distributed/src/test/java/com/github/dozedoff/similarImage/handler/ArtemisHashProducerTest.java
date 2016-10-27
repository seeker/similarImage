package com.github.dozedoff.similarImage.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.handler.ArtemisHashProducer;
import com.github.dozedoff.similarImage.messaging.QueryMessage;

@RunWith(MockitoJUnitRunner.class)
public class ArtemisHashProducerTest {
	private static final String TEST_ADDRESS = "test";

	@Mock
	private ClientSession session;

	@Mock
	private ClientProducer producer;

	@Mock
	private ClientMessage message;

	@Mock
	private QueryMessage query;

	private Path testFile;

	private ArtemisHashProducer cut;

	@Before
	public void setUp() throws Exception {
		testFile = Files.createTempFile(ArtemisHashProducerTest.class.getSimpleName(), null);

		when(session.createProducer(TEST_ADDRESS)).thenReturn(producer);
		when(session.createMessage(any(Boolean.class))).thenReturn(message);

		cut = new ArtemisHashProducer(session, TEST_ADDRESS);
	}

	@Test
	public void testHandleInputStreamSet() throws Exception {
		cut.handle(testFile);

		verify(message).setBodyInputStream(any());
	}

	@Test
	public void testHandleTaskTypeSet() throws Exception {
		cut.handle(testFile);

		verify(message).putStringProperty(ArtemisHashProducer.MESSAGE_TASK_PROPERTY, ArtemisHashProducer.MESSAGE_TASK_VALUE_HASH);
	}

	@Test
	public void testHandleFilePathSet() throws Exception {
		cut.handle(testFile);

		verify(message).putStringProperty(ArtemisHashProducer.MESSAGE_PATH_PROPERTY, testFile.toString());
	}

	@Test
	public void testHandleMessageSent() throws Exception {
		cut.handle(testFile);

		verify(producer).send(message);
	}

	@Test
	public void testHandleAMQerror() throws Exception {
		Mockito.doThrow(new ActiveMQException()).when(producer).send(message);

		assertThat(cut.handle(testFile), is(false));
	}

	@Test
	public void testHandleIOerror() throws Exception {
		FileSystem fs = Mockito.mock(FileSystem.class);
		Path file = Mockito.mock(Path.class);
		FileSystemProvider fsp  = Mockito.mock(FileSystemProvider.class);

		when(file.getFileSystem()).thenReturn(fs);
		when(fs.provider()).thenReturn(fsp);
		when(fsp.newInputStream(any(Path.class), anyVararg())).thenThrow(new IOException());

		assertThat(cut.handle(file), is(false));
	}

	@Test
	public void testImageAlreadyPendingHandled() throws Exception {
		when(query.pendingImagePaths()).thenReturn(Arrays.asList(testFile.toString()));
		cut = new ArtemisHashProducer(session, TEST_ADDRESS, query);

		assertThat(cut.handle(testFile), is(true));
	}

	@Test
	public void testImageAlreadyPendingNoMessageSent() throws Exception {
		when(query.pendingImagePaths()).thenReturn(Arrays.asList(testFile.toString()));
		cut = new ArtemisHashProducer(session, TEST_ADDRESS, query);

		verify(producer, never()).send(any(ClientMessage.class));
	}
}
