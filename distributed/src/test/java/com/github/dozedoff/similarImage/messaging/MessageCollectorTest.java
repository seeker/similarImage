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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageCollectorTest {
	private static final int MESSAGE_THRESHOLD = 5;

	@Mock
	private CollectedMessageConsumer collector;

	@Mock
	private CollectedMessageConsumer collector2;

	@Mock
	private ClientMessage message;

	@Captor
	private ArgumentCaptor<List<ClientMessage>> drainedValues;

	private MessageCollector cut;

	@Before
	public void setUp() throws Exception {
		cut = new MessageCollector(MESSAGE_THRESHOLD, collector, collector2);
	}

	private void addNumberOfMessages(int messageCount) {
		for (int i = 0; i < messageCount; i++) {
			cut.addMessage(message);
		}
	}

	@Test
	public void testAddMessageBelowThreshold() throws Exception {
		cut.addMessage(message);

		verify(collector, never()).onDrain(anyListOf(ClientMessage.class));
	}

	@Test
	public void testAddMessageAboveThreshold() throws Exception {
		addNumberOfMessages(MESSAGE_THRESHOLD);

		verify(collector).onDrain(anyListOf(ClientMessage.class));
	}

	@Test
	public void testAddMessageListWasCleared() throws Exception {
		addNumberOfMessages(MESSAGE_THRESHOLD + 2);
		cut.drain();

		verify(collector, times(2)).onDrain(drainedValues.capture());

		assertThat(drainedValues.getValue().size(), is(2));
	}

	@Test
	public void testAddMessageReTrigger() throws Exception {
		addNumberOfMessages(2 * MESSAGE_THRESHOLD);

		verify(collector, times(2)).onDrain(anyListOf(ClientMessage.class));
	}

	@Test
	public void testDrainToCollector() throws Exception {
		cut.drain();

		verify(collector).onDrain(anyListOf(ClientMessage.class));
	}

	@Test
	public void testDrainToCollector2() throws Exception {
		cut.drain();

		verify(collector2).onDrain(anyListOf(ClientMessage.class));
	}

	@Test
	public void testDrainedValuesCount() throws Exception {
		addNumberOfMessages(MESSAGE_THRESHOLD-1);
		cut.drain();

		verify(collector2).onDrain(drainedValues.capture());
		
		assertThat(drainedValues.getValue().size(), is(MESSAGE_THRESHOLD - 1));
	}
}
