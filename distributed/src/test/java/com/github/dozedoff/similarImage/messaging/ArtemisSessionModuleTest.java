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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class ArtemisSessionModuleTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Mock
	private ClientSessionFactory factory;

	@Mock
	private ClientSession session;

	private ArtemisSession cut;

	@Before
	public void setUp() throws Exception {
		cut = new ArtemisSession(factory);
	}

	@Test
	public void testNormalSessionCreated() throws Exception {
		when(factory.createSession()).thenReturn(session);

		cut.getSession();

		verify(factory).createSession();
	}

	@Test
	public void testNormalSessionStarted() throws Exception {
		when(factory.createSession()).thenReturn(session);

		cut.getSession();

		verify(session).start();
	}

	@Test
	public void testTransactedSessionCreated() throws Exception {
		when(factory.createTransactedSession()).thenReturn(session);

		cut.getTransactedSession();

		verify(factory).createTransactedSession();
	}

	@Test
	public void testTransactedSessionStarted() throws Exception {
		when(factory.createTransactedSession()).thenReturn(session);

		cut.getTransactedSession();

		verify(session).start();
	}

	@Test
	public void testFactoryClosed() throws Exception {
		cut.close();

		verify(factory).close();
	}
}
