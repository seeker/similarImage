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
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;

//FIXME Silent runner is just a band-aid to get the tests to run
@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoryNodeTest extends MessagingBaseTest {
	private static final Path PATH = Paths.get("foo");
	private static final UUID UUID = new UUID(42L, 24L);
	private static final String RETURN_ADDRESS = "return";

	@Mock
	private PendingHashImageRepository pendingRepository;

	@Mock
	private TaskMessageHandler taskMessageHandler;

	private MessageFactory messageFactory;
	private MetricRegistry metrics;

	private RepositoryNode cut;

	@Before
	public void setUp() throws Exception {
		when(pendingRepository.store(any(PendingHashImage.class))).thenReturn(true);
		when(pendingRepository.getAll()).thenReturn(Arrays.asList(new PendingHashImage(PATH, UUID)));
		when(session.createConsumer(any(String.class), any(String.class))).thenReturn(consumer);
		message.putStringProperty(ClientMessageImpl.REPLYTO_HEADER_NAME.toString(), RETURN_ADDRESS);

		messageFactory = new MessageFactory(session);
		metrics = new MetricRegistry();
		cut = new RepositoryNode(session, pendingRepository, taskMessageHandler, metrics);
	}

	@Test
	public void testQueryPending() throws Exception {
		message = messageFactory.pendingImageQuery();

		cut.onMessage(message);
		// TODO test actual message contents
		assertThat(sessionMessage.getBodySize(), greaterThan(0));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("RepositoryNode"));
	}

	@Test
	public void testStop() throws Exception {
		cut.stop();

		verify(consumer, times(2)).close();
		verify(producer).close();
	}
}
