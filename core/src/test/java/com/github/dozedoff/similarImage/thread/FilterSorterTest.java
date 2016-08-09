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
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

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
import com.github.dozedoff.similarImage.event.GuiEventBus;
import com.github.dozedoff.similarImage.util.StringUtil;
import com.google.common.eventbus.Subscribe;

@RunWith(MockitoJUnitRunner.class)
public class FilterSorterTest {
	private static final String TAG_LANDSCAPE = "landscape";
	private static final String TAG_SUNSET = "sunset";
	private static final String TAG_EXCEPTION = "exception";

	private static final long HASH_THREE = 3;

	private static final int SEARCH_DISTANCE = 0;

	private static final String EXCEPTION_MESSAGE = "Testig...";

	@Mock
	private Persistence persistenceMock;

	private List<ImageRecord> records;
	private List<Long> result;
	private FilterSorter cut ;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		GuiEventBus.getInstance().register(this);

		createRecords();

		when(persistenceMock.getAllRecords()).thenReturn(records);
		when(persistenceMock.getAllFilters(TAG_LANDSCAPE))
				.thenReturn(Arrays.asList(new FilterRecord(0, TAG_LANDSCAPE)));
		when(persistenceMock.getAllFilters(StringUtil.MATCH_ALL_TAGS))
				.thenReturn(Arrays.asList(new FilterRecord(0, TAG_LANDSCAPE), new FilterRecord(1, TAG_SUNSET)));

		when(persistenceMock.getAllFilters(TAG_EXCEPTION)).thenThrow(new SQLException(EXCEPTION_MESSAGE));
	}

	/**
	 * Listen for deprecated group events.
	 * 
	 * @param event
	 *            deprecated group list
	 */
	@Subscribe
	public void listenGroupEvent(List<Long> event) {
		result = event;
	}

	private void createRecords() {
		records = new LinkedList<>();

		records.add(new ImageRecord("foo", 0));
		records.add(new ImageRecord("bar", 1));
		records.add(new ImageRecord("foobar", HASH_THREE));
	}

	private void runCutAndWaitForFinish() throws InterruptedException {
		cut.start();
		cut.join();
	}

	@Test
	public void testFilterForSingleTag() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_LANDSCAPE, persistenceMock);
		runCutAndWaitForFinish();

		assertThat(result, containsInAnyOrder(0L));
	}

	@Test
	public void testFilterForMatchAllTag() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, StringUtil.MATCH_ALL_TAGS, persistenceMock);
		runCutAndWaitForFinish();

		assertThat(result, containsInAnyOrder(0L, 1L));
	}

	@Test
	public void testFilterLoadException() throws Exception {
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_EXCEPTION, persistenceMock);
		runCutAndWaitForFinish();

		assertThat(result, is(empty()));
	}

	@Test
	public void testRecordLoadException() throws Exception {
		when(persistenceMock.getAllRecords()).thenThrow(new SQLException(EXCEPTION_MESSAGE));
		
		cut = new FilterSorter(SEARCH_DISTANCE, TAG_SUNSET, persistenceMock);
		runCutAndWaitForFinish();

		assertThat(result, is(empty()));
	}
}
