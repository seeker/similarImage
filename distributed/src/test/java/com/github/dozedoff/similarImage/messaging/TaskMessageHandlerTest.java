package com.github.dozedoff.similarImage.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;

@RunWith(MockitoJUnitRunner.class)
public class TaskMessageHandlerTest extends MessagingBaseTest {
	private static final int TEST_ID = 1;
	private static final long TEST_HASH = 42L;
	private static final String TEST_PATH = "foo";
	private static final UUID UUID = new UUID(99, 100);
	private static final long MOST = UUID.getMostSignificantBits();
	private static final long LEAST = UUID.getLeastSignificantBits();

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private PendingHashImageRepository pendingRepository;

	private TaskMessageHandler cut;

	private MessageFactory messageFactory;

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
		cut = new TaskMessageHandler(pendingRepository, imageRepository, session);
	}

	@Test
	public void testStoreHash() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(new PendingHashImage(TEST_PATH, MOST, LEAST));

		cut.onMessage(message);

		verify(imageRepository).store(new ImageRecord(TEST_PATH, TEST_HASH));
	}

	@Test
	public void testSendEaUpdate() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, MOST, LEAST);
		when(pendingRepository.getByUUID(MOST, LEAST)).thenReturn(new PendingHashImage(TEST_PATH, MOST, LEAST));

		cut.onMessage(message);

		verify(producer).send(messageFactory.eaUpdate(Paths.get(TEST_PATH), TEST_HASH));
	}
}
