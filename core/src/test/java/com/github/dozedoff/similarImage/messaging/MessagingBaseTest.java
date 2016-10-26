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
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.ActiveMQBuffer;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public abstract class MessagingBaseTest {
	@Mock
	protected ClientSession session;
	@Mock
	protected ClientProducer producer;
	@Mock
	protected ClientConsumer consumer;
	/**
	 * Message that arrived at the consumer
	 */
	@Mock
	protected ClientMessage message;
	/**
	 * Message created by the session
	 */
	@Mock
	protected ClientMessage sessionMessage;
	@Mock
	protected ActiveMQBuffer bodyBuffer;

	@Mock
	protected ActiveMQBuffer sessionBodyBuffer;

	@Before
	public void messagingSetup() throws ActiveMQException {
		sessionMessage = new ClientMessageImpl(ClientMessageImpl.DEFAULT_TYPE, false, 0, 0, (byte) 0, 0);

		when(session.createConsumer(any(String.class))).thenReturn(consumer);
		when(session.createProducer(any(String.class))).thenReturn(producer);
		when(session.createProducer()).thenReturn(producer);
		when(session.createMessage(any(Boolean.class))).thenReturn(sessionMessage);
	}
}
