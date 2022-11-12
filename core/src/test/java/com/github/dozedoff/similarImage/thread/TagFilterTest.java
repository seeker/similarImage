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
package com.github.dozedoff.similarImage.thread;

import org.junit.Rule;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.quality.Strictness;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.duplicate.RecordSearch;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class TagFilterTest {
	public @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	private static final String PATH1 = "foo";
	private static final String PATH2 = "bar";
	private static final int DISTANCE = 0;
	private static final Tag TAG = new Tag("test");
	private static final Tag TAG_ALL = new Tag(StringUtil.MATCH_ALL_TAGS);

	@Mock
	private FilterRepository filterRepository;

	@InjectMocks
	private TagFilter cut;

	private RecordSearch recordSearch;

	private Multimap<Long, ImageRecord> emptyMultimap;

	private ImageRecord image1;
	private ImageRecord image2;

	private FilterRecord filter1;
	private FilterRecord filter2;

	private void createImages() {
		image1 = new ImageRecord(PATH1, 1);
		image2 = new ImageRecord(PATH2, 2);
	}

	private void createFilters() {
		filter1 = new FilterRecord(1, TAG);
		filter2 = new FilterRecord(2, new Tag(""));
	}

	@Before
	public void setUp() throws Exception {
		createImages();
		createFilters();

		emptyMultimap = MultimapBuilder.hashKeys().hashSetValues().build();

		recordSearch = new RecordSearch();
		recordSearch.build(Arrays.asList(new ImageRecord[] { image1, image2 }));
	}

	@Test
	public void testRepositoryException() throws Exception {
		when(filterRepository.getAll()).thenThrow(new RepositoryException("just testing!"));

		assertThat(cut.getFilterMatches(recordSearch, TAG_ALL, DISTANCE), is(emptyMultimap));
	}

	@Test
	public void testMatchingTag() throws Exception {
		when(filterRepository.getByTag(TAG)).thenReturn(Arrays.asList(new FilterRecord[] { filter1 }));

		assertThat(cut.getFilterMatches(recordSearch, TAG, DISTANCE).get(1L), hasItem(image1));
	}

	@Test
	public void testMatchingTagSecondImageNotIncluded() throws Exception {
		when(filterRepository.getByTag(TAG)).thenReturn(Arrays.asList(new FilterRecord[] { filter1 }));

		assertThat(cut.getFilterMatches(recordSearch, TAG, DISTANCE).get(1L), not(hasItem(image2)));
	}
}
