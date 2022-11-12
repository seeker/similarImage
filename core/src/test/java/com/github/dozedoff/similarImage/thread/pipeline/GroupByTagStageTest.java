/*  Copyright (C) 2017  Nicholas Wright
    
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
package com.github.dozedoff.similarImage.thread.pipeline;

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class GroupByTagStageTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final Tag TAG = new Tag("foo");
	private static final long HASH_A = 0;
	private static final long HASH_B = 1;
	private static final Multimap<Long, ImageRecord> EMPTY_MAP = ImmutableMultimap.of();

	@Mock
	private FilterRepository filterRepository;

	private GroupByTagStage cut;

	private ImageRecord imageA;
	private ImageRecord imageB;
	private ImageRecord imageC;

	private FilterRecord filterA;

	private List<ImageRecord> images;

	@Before
	public void setUp() throws Exception {
		imageA = createImage(HASH_A);
		imageB = createImage(HASH_B);
		imageC = createImage(HASH_B);

		filterA = new FilterRecord(HASH_A, TAG);

		images = Arrays.asList(new ImageRecord[] { imageA, imageB, imageC });
		
		when(filterRepository.getByTag(TAG)).thenReturn(Arrays.asList(filterA));

		cut = new GroupByTagStage(filterRepository, TAG);
	}

	private ImageRecord createImage(long hash) {
		return new ImageRecord(String.valueOf(hash), hash);
	}

	@Test
	public void testGroupedByTag() throws Exception {
		assertThat(cut.apply(images).get(HASH_A), containsInAnyOrder(imageA));
	}

	@Test
	public void testTagsWithDistance() throws Exception {
		cut = new GroupByTagStage(filterRepository, TAG, 1);

		assertThat(cut.apply(images).get(HASH_A), containsInAnyOrder(imageA, imageB));
	}

	@Test
	public void testRepositoryError() throws Exception {
		when(filterRepository.getByTag(TAG)).thenThrow(new RepositoryException(""));

		assertThat(cut.apply(images), is(EMPTY_MAP));
	}
}
