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

import java.nio.file.Paths;
import java.sql.SQLException;
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
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.db.repository.FilterRepository;
import com.github.dozedoff.similarImage.db.repository.RepositoryException;
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.event.GuiGroupEvent;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.Subscribe;

@RunWith(MockitoJUnitRunner.class)
public class FilterSorterTest {
	private static final String TAG_LANDSCAPE = "landscape";
	private static final String TAG_SUNSET = "sunset";
	private static final String TAG_EXCEPTION = "exception";

	private static final String PATH_ZERO = "foo";

	private static final long HASH_THREE = 3;

	private static final int SEARCH_DISTANCE = 0;

	private static final String EXCEPTION_MESSAGE = "Testig...";

	@Mock
	private Persistence persistenceMock;
	
	@Mock
	private FilterRepository filterRepository;

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

		when(persistenceMock.getAllRecords()).thenReturn(records);
		when(persistenceMock.filterByPath(Paths.get(PATH_ZERO))).thenReturn(Arrays.asList(new ImageRecord(PATH_ZERO, 0)));
		when(filterRepository.getFiltersByTag(TAG_LANDSCAPE))
				.thenReturn(Arrays.asList(new FilterRecord(0, TAG_LANDSCAPE)));

		when(filterRepository.getFiltersByTag(TAG_EXCEPTION)).thenThrow(new RepositoryException(EXCEPTION_MESSAGE));
		when(filterRepository.getAllFilters())
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

		records.add(new ImageRecord(PATH_ZERO, 0));
		records.add(new ImageRecord("bar", 1));
		records.add(new ImageRecord("foobar", HASH_THREE));
	}

	private void runCutAndWaitForFinish() throws InterruptedException {
		cut.start();
		cut.join();
	}

	@Test
	public void testFilterForSingleTag() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, persistenceMock, filterRepository);
		runCutAndWaitForFinish();

		assertThat(result.get(0L), containsInAnyOrder(records.get(0)));
	}

	@Test
	public void testFilterForMatchAllTag() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, StringUtil.MATCH_ALL_TAGS, persistenceMock, filterRepository);
		runCutAndWaitForFinish();

		assertThat(result.get(0L), containsInAnyOrder(records.get(0)));
		assertThat(result.get(1L), containsInAnyOrder(records.get(1)));
	}

	@Test
	public void testFilterLoadException() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_EXCEPTION, persistenceMock, filterRepository);
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testRecordLoadException() throws Exception {
		when(persistenceMock.getAllRecords()).thenThrow(new SQLException(EXCEPTION_MESSAGE));

		cut = new FilterSorter(SEARCH_DISTANCE, TAG_SUNSET, persistenceMock, filterRepository);
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testFilterByPathNoMatch() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, persistenceMock, filterRepository, Paths.get("bar"));
		runCutAndWaitForFinish();

		assertThat(result, is(emptyMultiMap));
	}

	@Test
	public void testFilterByPath() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, persistenceMock, filterRepository, Paths.get(PATH_ZERO));
		runCutAndWaitForFinish();

		assertThat(result.get(0L), containsInAnyOrder(records.get(0)));
	}
}
