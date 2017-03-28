package com.github.dozedoff.similarImage.messaging;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.io.ExtendedAttributeQuery;
import com.github.dozedoff.similarImage.io.HashAttribute;

@RunWith(MockitoJUnitRunner.class)
public class StorageNodeTest extends MessagingBaseTest {
	private static final Path PATH = Paths.get("foo");
	private static final long HASH = 42;

	@Mock
	private ExtendedAttributeQuery eaQuery;

	@Mock
	private HashAttribute hashAttribute;

	private StorageNode cut;
	private MessageFactory messageFactory;
	private Path testFile;
	private Path cachedFile;

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
		testFile = Files.createTempFile("StorageNodeTest", null);
		cachedFile = Files.createTempFile("StorageNodeTest", null);

		cut = new StorageNode(session, eaQuery, hashAttribute, Arrays.asList(cachedFile));

	}

	@After
	public void tearDown() throws Exception {
		Files.deleteIfExists(testFile);
		Files.deleteIfExists(cachedFile);
	}

	@Test
	public void testOnMessageEaUpdate() throws Exception {
		message = messageFactory.eaUpdate(PATH, HASH);

		cut.onMessage(message);

		verify(hashAttribute).writeHash(PATH, HASH);
	}

	@Test
	public void testOnMessageCorrupt() throws Exception {
		message = messageFactory.corruptMessage(PATH);

		cut.onMessage(message);

		verify(hashAttribute).markCorrupted(PATH);
	}

	@Test
	public void testOnMessageWrong() throws Exception {
		message = messageFactory.pendingImageQuery();

		cut.onMessage(message);

		verify(hashAttribute, never()).writeHash(PATH, HASH);
		verify(hashAttribute, never()).markCorrupted(PATH);
	}

	@Test
	public void testProcessFile() throws Exception {
		cut.processFile(testFile);

		verify(producer).send(messageFactory.resizeRequest(testFile, Files.newInputStream(testFile)));
	}

	@Test
	public void testProcessFileSkipDuplicate() throws Exception {
		cut.processFile(testFile);
		cut.processFile(testFile);

		verify(producer, times(1)).send(messageFactory.resizeRequest(testFile, Files.newInputStream(testFile)));
	}

	@Test
	public void testProcessFileSkipDuplicateCached() throws Exception {
		cut.processFile(cachedFile);

		verify(producer, never()).send(messageFactory.resizeRequest(testFile, Files.newInputStream(testFile)));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("StorageNode"));
	}

	@Test
	public void testStop() throws Exception {
		cut.stop();

		verify(consumer).close();
		verify(producer).close();
	}
}
