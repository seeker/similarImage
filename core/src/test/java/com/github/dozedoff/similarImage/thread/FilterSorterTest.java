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
package com.github.dozedoff.similarImage.thread;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Tag;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.ImageRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.db.repository.TagRepository;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.Subscribe;

@RunWith(MockitoJUnitRunner.class)
public class FilterSorterTest {
	private static final Tag TAG_LANDSCAPE = new Tag("landscape");
	private static final Tag TAG_SUNSET = new Tag("sunset");
	private static final Tag TAG_EXCEPTION = new Tag("exception");

	private static final String PATH_ZERO = "foo";
	private static final String PATH_ONE = "bar";
	private static final String PATH_THREE = "foobar";

	private static final long HASH_ONE = 1;
	private static final long HASH_ZERO = 0;
	private static final long HASH_THREE = 3;

	private static final int SEARCH_DISTANCE = 0;

	private static final String EXCEPTION_MESSAGE = "Testig...";

	@Mock
	private ImageRepository imageRepository;
	
	@Mock
	private FilterRepository filterRepository;

	@Mock
	private TagRepository tagRepository;

	private List<ImageRecord> records;
	private Multimap<Long, ImageRecord> result;
	private Multimap<Long, ImageRecord> emptyMultiMap = MultimapBuilder.hashKeys().hashSetValues().build();
	private FilterSorter cut;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		GuiEventBus.getInstance().register(this);

		result = MultimapBuilder.hashKeys().hashSetValues().build();
		emptyMultiMap.clear();

		createRecords();

		when(imageRepository.getAll()).thenReturn(records);
		when(imageRepository.startsWithPath(Paths.get(PATH_ZERO)))
				.thenReturn(Arrays.asList(new ImageRecord(PATH_ZERO, 0)));
		when(filterRepository.getByTag(TAG_LANDSCAPE))
				.thenReturn(Arrays.asList(new FilterRecord(0, TAG_LANDSCAPE)));

		when(filterRepository.getByTag(TAG_EXCEPTION)).thenThrow(new RepositoryException(EXCEPTION_MESSAGE));
		when(filterRepository.getAll())
				.thenReturn(Arrays.asList(new FilterRecord(0, TAG_LANDSCAPE), new FilterRecord(1, TAG_SUNSET)));
	}

	/**
	 * Listen for group update events.
	 * 
	 * @param event
	 *            containing the new group information
	 */
	@Subscribe
	public void listenGroupEvent(GuiGroupEvent event) {
		result = event.getGroups();
	}

	private void createRecords() {
		records = new LinkedList<>();

		records.add(new ImageRecord(PATH_ZERO, HASH_ZERO));
		records.add(new ImageRecord(PATH_ONE, HASH_ONE));
		records.add(new ImageRecord(PATH_THREE, HASH_THREE));
	}

	private FilterSorter createSorter(int hammingdistance, Tag tag, Path scope) {
		return new FilterSorter(hammingdistance, tag, filterRepository, imageRepository, scope);
	}

	private FilterSorter createSorter(int hammingdistance, Tag tag) {
		return new FilterSorter(hammingdistance, tag, filterRepository, imageRepository);
	}

	private void runCutAndWaitForFinish() throws InterruptedException {
		cut.start();
		cut.join();
	}

	@Test
	public void testFilterForSingleTag() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, TAG_LANDSCAPE);
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ZERO), containsInAnyOrder(records.get(0)));
	}

	@Test
	public void testFilterForMatchAllTag() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, new Tag(StringUtil.MATCH_ALL_TAGS));
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ZERO), containsInAnyOrder(records.get(0)));
		assertThat(result.get(HASH_ONE), containsInAnyOrder(records.get(1)));
	}

	@Test
	public void testFilterLoadException() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, TAG_EXCEPTION);
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testRecordLoadException() throws Exception {
		when(imageRepository.getAll()).thenThrow(new RepositoryException(EXCEPTION_MESSAGE));

		cut = createSorter(SEARCH_DISTANCE, TAG_SUNSET);
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testFilterByPathNoMatch() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, Paths.get(PATH_ONE));
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testFilterByPath() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, Paths.get(PATH_ZERO));
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ZERO), containsInAnyOrder(records.get(0)));
	}

	@Test
	public void testFilterWithNullTag() throws Exception {
		cut = createSorter(SEARCH_DISTANCE, null);
		runCutAndWaitForFinish();

		assertThat(result.get(HASH_ZERO), containsInAnyOrder(records.get(0)));
		assertThat(result.get(HASH_ONE), containsInAnyOrder(records.get(1)));
	}
}
