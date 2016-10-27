package com.github.dozedoff.similarImage.messaging;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Ignore;
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

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
		cut = new StorageNode(session, eaQuery, hashAttribute);
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

	@Ignore
	@Test
	public void testProcessFile() throws Exception {
		throw new RuntimeException("not yet implemented");
	}

}
