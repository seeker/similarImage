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
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResultMessageSinkTest {
	private static final int VERIFY_TIMEOUT = 500;
	private static final int TEST_TIMEOUT = 2000;

	@Mock
	private MessageCollector collector;

	@Mock
	private ClientSession session;

	@Mock
	private ClientConsumer consumer;

	@Mock
	private ClientMessage message;

	private ResultMessageSink cut;

	@Before
	public void setUp() throws Exception {
		when(consumer.receive(anyLong())).thenReturn(message, (ClientMessage) null);
		when(session.createConsumer(anyString(), anyString())).thenReturn(consumer);

		cut = new ResultMessageSink(session, collector);
	}

	@After
	public void tearDown() {
		cut.stop();
	}

	@Test
	public void testReceiveMessage() throws Exception {
		verify(consumer, timeout(VERIFY_TIMEOUT).atLeastOnce()).receive(anyLong());
	}

	@Test
	public void testStoreMessage() throws Exception {
		verify(collector, timeout(VERIFY_TIMEOUT)).addMessage(message);
	}

	@Test
	public void testStop() throws Exception {
		cut.stop();

		verify(collector, timeout(VERIFY_TIMEOUT)).drain();
		verify(consumer).close();
	}

	@Test(timeout = TEST_TIMEOUT)
	public void testDrainOnNoMessage() throws Exception {
		cut.stop();
		Mockito.reset(collector);
		when(consumer.receive(anyLong())).thenReturn(null);

		cut = new ResultMessageSink(session, collector);

		verify(collector, timeout(VERIFY_TIMEOUT).atLeastOnce()).drain();
		verify(collector, never()).addMessage(message);
	}

	@Test
	public void testMessageReadFailure() throws Exception {
		cut.stop();
		Mockito.reset(collector);

		when(consumer.receive(anyLong())).thenThrow(new ActiveMQException("test"));

		cut = new ResultMessageSink(session, collector);

		verify(collector, atMost(1)).drain();
		verify(collector, never()).addMessage(any(ClientMessage.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonTransactedSession() throws Exception {
		when(session.isAutoCommitAcks()).thenReturn(true);

		cut = new ResultMessageSink(session, collector);
	}
}
