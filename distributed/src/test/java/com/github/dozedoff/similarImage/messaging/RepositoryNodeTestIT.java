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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.lenient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import com.codahale.metrics.MetricRegistry;
import com.github.dozedoff.similarImage.db.PendingHashImage;
import com.github.dozedoff.similarImage.db.repository.PendingHashImageRepository;

public class RepositoryNodeTestIT extends MessagingBaseTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final Path PATH = Paths.get("foo");
	private static final UUID UUID = new UUID(42L, 24L);

	@Mock
	private PendingHashImageRepository pendingRepository;

	@Mock
	private TaskMessageHandler taskMessageHandler;

	private MetricRegistry metrics;

	private RepositoryNode cut;
	private QueryMessage queryMessage;

	@Before
	public void setUp() throws Exception {
		lenient().when(pendingRepository.getAll()).thenReturn(Arrays.asList(new PendingHashImage(PATH, UUID)));

		metrics = new MetricRegistry();
		cut = new RepositoryNode(session, pendingRepository, taskMessageHandler, metrics);
		queryMessage = new QueryMessage(session);
	}

	@Test
	public void testQueryPending() throws Exception {
		List<String> pending = queryMessage.pendingImagePaths();
		
		assertThat(pending, hasItem(PATH.toString()));
	}
	
	@Test
	public void testQueryPendingOnlyOnePath() throws Exception {
		List<String> pending = queryMessage.pendingImagePaths();
		
		assertThat(pending, hasSize(1));
	}

	@Test
	public void testToString() throws Exception {
		assertThat(cut.toString(), is("RepositoryNode"));
	}
}
