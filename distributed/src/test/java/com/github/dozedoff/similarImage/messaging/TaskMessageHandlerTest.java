package com.github.dozedoff.similarImage.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
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

	@Mock
	private ImageRepository imageRepository;

	@Mock
	private PendingHashImageRepository pendingRepository;

	@InjectMocks
	private TaskMessageHandler cut;

	private MessageFactory messageFactory;

	@Before
	public void setUp() throws Exception {
		messageFactory = new MessageFactory(session);
	}

	@Test
	public void testStoreHash() throws Exception {
		message = messageFactory.resultMessage(TEST_HASH, TEST_ID);
		when(pendingRepository.getById(TEST_ID)).thenReturn(new PendingHashImage(TEST_PATH));

		cut.onMessage(message);

		verify(imageRepository).store(new ImageRecord(TEST_PATH, TEST_HASH));
	}
}
