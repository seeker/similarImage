/*  Copyright (C) 2014  Nicholas Wright
    
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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.dozedoff.similarImage.db.FilterRecord;
import com.github.dozedoff.similarImage.db.ImageRecord;
import com.github.dozedoff.similarImage.db.Persistence;
import com.github.dozedoff.similarImage.duplicate.SortSimilar;
import com.github.dozedoff.similarImage.gui.SimilarImageView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(MockitoJUnitRunner.class)
public class FilterSorterTest {
	@Mock
	private SimilarImageView gui;

	@Mock
	private Persistence persistence;

	@Mock
	private SortSimilar sorter;

	private List<ImageRecord> records;
	private FilterSorter filterSorter ;

	private static final String ERROR_MSG = "This is a test";

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		createRecords();
		when(persistence.getAllRecords()).thenReturn(records);

		filterSorter = new FilterSorter(2, "test", gui, sorter, persistence);
	}

	private void createRecords() {
		records = new LinkedList<>();

		records.add(new ImageRecord("foo", 0));
		records.add(new ImageRecord("bar", 1));
		records.add(new ImageRecord("foobar", 3));
	}

	@Test
	public void testRunWithReason() throws Exception {
		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(persistence).getAllRecords();
		inOrder.verify(persistence).getAllFilters("test");
		inOrder.verify(sorter).sortFilter(eq(2), eq("test"), eq(records), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunWithBlankReason() throws Exception {
		String reason = "";
		filterSorter = new FilterSorter(2, reason, gui, sorter, persistence);

		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(persistence).getAllRecords();
		inOrder.verify(persistence).getAllFilters();
		inOrder.verify(sorter).sortFilter(eq(2), eq(reason), eq(records), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	@SuppressFBWarnings(value = "NP_LOAD_OF_KNOWN_NULL_VALUE", justification = "Null as reason is a valid parameter")
	public void testRunWithNullReason() throws Exception {
		String reason = null;
		filterSorter = new FilterSorter(2, reason, gui, sorter, persistence);

		// TODO create constructor FilterSorter(int hammingDistance,
		// SimilarImageView gui, SortSimilar sorter, Persistence persistence)

		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(persistence).getAllRecords();
		inOrder.verify(persistence).getAllFilters();
		inOrder.verify(sorter).sortFilter(eq(2), eq(reason), eq(records), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunWithAsteriskReason() throws Exception {
		String reason = "*";
		filterSorter = new FilterSorter(2, reason, gui, sorter, persistence);

		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(persistence).getAllRecords();
		inOrder.verify(persistence).getAllFilters();
		inOrder.verify(sorter).sortFilter(eq(2), eq(reason), eq(records), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));
	}

	@Test
	public void testRunSQLExceptionWithReason() throws Exception {
		String reason = "*";

		when(persistence.getAllRecords()).thenThrow(new SQLException(ERROR_MSG));

		filterSorter = new FilterSorter(2, reason, gui, sorter, persistence);

		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(sorter).sortFilter(eq(2), eq(reason), eq(new LinkedList<ImageRecord>()), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));

		verify(persistence, never()).getAllFilters();
	}

	@Test
	public void testRunSQLExceptionNoReason() throws Exception {
		String reason = "foo";

		when(persistence.getAllRecords()).thenThrow(new SQLException(ERROR_MSG));

		filterSorter = new FilterSorter(2, reason, gui, sorter, persistence);

		filterSorter.start();
		filterSorter.join();

		InOrder inOrder = Mockito.inOrder(gui, persistence, sorter);

		inOrder.verify(gui).setStatus(eq("Sorting..."));
		inOrder.verify(sorter).sortFilter(eq(2), eq(reason), eq(new LinkedList<ImageRecord>()), anyListOf(FilterRecord.class));
		inOrder.verify(gui).setStatus(anyString());
		inOrder.verify(sorter).getDuplicateGroups();
		inOrder.verify(gui).populateGroupList(anyListOf(Long.class));

		verify(persistence, never()).getAllFilters(reason);
	}
}
